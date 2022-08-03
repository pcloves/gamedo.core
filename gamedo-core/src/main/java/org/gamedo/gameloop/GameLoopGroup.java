package org.gamedo.gameloop;

import lombok.extern.log4j.Log4j2;
import org.gamedo.util.Hashing;
import org.gamedo.util.function.EntityFunction;
import org.gamedo.util.function.EntityPredicate;
import org.gamedo.exception.GameLoopException;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.logging.Markers;
import org.gamedo.util.Pair;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
public class GameLoopGroup implements IGameLoopGroup {
    private static final Hashing HASHING;

    private final String id;
    private final int nodeCountPerGameLoop;
    private final AtomicInteger idx = new AtomicInteger(0);
    private final AtomicReference<Data> dataAtomicReference = new AtomicReference<>();

    private volatile List<IGameLoop> gameLoopList = new ArrayList<>();

    static {
        HASHING = Hashing.FNV1A;
    }

    private static class Data {
        private final List<IGameLoop> gameLoopList = new ArrayList<>();
        private final TreeMap<Integer, IGameLoop> gameLoopTreeMap = new TreeMap<>();

        private Data(List<IGameLoop> gameLoopList, int nodeCountPerGameLoop) {

            this.gameLoopList.addAll(gameLoopList);
            this.gameLoopList.forEach(gameLoop -> {
                for (int i = 0; i < nodeCountPerGameLoop; i++) {
                    final int hash = HASHING.hash(gameLoop.getId() + '-' + i + '-' + UUID.randomUUID());
                    gameLoopTreeMap.put(hash, gameLoop);
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Data data = (Data) o;
            return Objects.equals(gameLoopList, data.gameLoopList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gameLoopList);
        }
    }

    public GameLoopGroup(String id, int nodeCountPerGameLoop, IGameLoop... gameLoops) {

        final List<List<IGameLoop>> duplicateList = Arrays.stream(gameLoops)
                .collect(Collectors.groupingBy(IGameLoop::getId))
                .values()
                .stream()
                .filter(list -> list.size() > 1)
                .collect(Collectors.toList());

        if (!duplicateList.isEmpty()) {
            log.error(Markers.GameLoop, "duplicate gameLoop:{}", duplicateList);
            throw new GameLoopException("duplicate gameLoop:" + duplicateList);
        }

        this.id = id;
        this.nodeCountPerGameLoop = nodeCountPerGameLoop;
        gameLoopList.addAll(Arrays.stream(gameLoops).collect(Collectors.toList()));
        dataAtomicReference.set(new Data(gameLoopList, nodeCountPerGameLoop));
    }

    public GameLoopGroup(String id, int gameLoopCount) {
        this(id, 10, IntStream.rangeClosed(1, gameLoopCount)
                .mapToObj(value -> new GameLoop(id + '-' + value))
                .toArray(GameLoop[]::new));
    }

    public GameLoopGroup(String id, int nodeCountPerGameLoop, int gameLoopCount) {
        this(id, nodeCountPerGameLoop, IntStream.rangeClosed(1, gameLoopCount)
                .mapToObj(value -> new GameLoop(id + '-' + value))
                .toArray(GameLoop[]::new));
    }

    @Override
    public void shutdown() {
        gameLoopList.forEach(ExecutorService::shutdown);
    }

    @Override
    public List<Runnable> shutdownNow() {

        return gameLoopList.stream()
                .map(ExecutorService::shutdownNow)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isShutdown() {
        return gameLoopList.stream().allMatch(IGameLoop::isShutdown);
    }

    @Override
    public boolean isTerminated() {
        return gameLoopList.stream().allMatch(IGameLoop::isTerminated);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {

        final boolean allTerminated = gameLoopList.stream()
                .parallel()
                .allMatch(iGameLoop -> {
                    try {
                        return iGameLoop.awaitTermination(timeout, unit);
                    } catch (InterruptedException e) {
                        return false;
                    }
                });

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return allTerminated;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return selectNext().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return selectNext().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return selectNext().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return selectNext().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return selectNext().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return selectNext().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return selectNext().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        selectNext().execute(command);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int size() {
        return gameLoopList.size();
    }

    @Override
    public boolean register(IGameLoop gameLoop) {

        //先检测是否已经注册
        if (gameLoopList.contains(gameLoop)) {
            return false;
        }

        //再cas无锁更新
        final Data data = dataAtomicReference.updateAndGet(old -> {

            //再次检测
            if (old.gameLoopList.contains(gameLoop)) {
                return old;
            }

            final int size = old.gameLoopList.size();
            //计算位置：当前位置的前一个位置，也就是说轮询一圈后才能被select到
            final int indexAdd = Math.abs((idx.get() + size) % (size + 1));

            final ArrayList<IGameLoop> gameLoopListNew = new ArrayList<>(old.gameLoopList);
            gameLoopListNew.add(indexAdd, gameLoop);

            return new Data(gameLoopListNew, nodeCountPerGameLoop);
        });

        //更新成功
        gameLoopList = data.gameLoopList;

        log.info(Markers.GameLoop,
                "register new gameLoop:{}, index:{}, count:{}",
                gameLoop::getId,
                () -> gameLoopList.indexOf(gameLoop),
                () -> gameLoopList.size());

        return true;
    }

    @Override
    public Optional<IGameLoop> select(String id) {
        return gameLoopList.stream()
                .filter(gameLoop -> gameLoop.getId().equals(id))
                .findFirst();
    }

    @Override
    public IGameLoop[] selectAll() {
        return gameLoopList.toArray(IGameLoop[]::new);
    }

    @Override
    public IGameLoop selectNext() {
        return gameLoopList.get(Math.abs(idx.getAndIncrement() % gameLoopList.size()));
    }

    @Override
    public IGameLoop selectHashing(String hashKey) {

        final int hashCode = HASHING.hash(hashKey);

        final TreeMap<Integer, IGameLoop> gameLoopTreeMap = dataAtomicReference.get().gameLoopTreeMap;
        //找到所有大于该hashCode的节点
        final SortedMap<Integer, IGameLoop> tailMap = gameLoopTreeMap.tailMap(hashCode);

        //没找到的话就返回第一个节点，否则就返回
        return tailMap.isEmpty() ? gameLoopTreeMap.get(gameLoopTreeMap.firstKey()) : gameLoopTreeMap.get(tailMap.firstKey());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Comparable<? super C>> CompletableFuture<List<IGameLoop>> select(EntityFunction<IGameLoop, C> chooser,
                                                                                       Comparator<C> comparator,
                                                                                       int limit) {

        List<IGameLoop> gameLoopNewList = new ArrayList<>(gameLoopList);
        final CompletableFuture<C>[] futureList = gameLoopNewList.stream()
                .map(gameLoop -> gameLoop.submit(chooser))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futureList)
                .thenApply(v -> IntStream.range(0, gameLoopNewList.size())
                        .mapToObj(i -> Pair.of(futureList[i].join(), gameLoopNewList.get(i)))
                        .sorted(Comparator.comparing(Pair::getK, comparator))
                        .limit(limit)
                        .map(Pair::getV)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public <R> CompletableFuture<R> submit(EntityFunction<IGameLoop, R> function) {
        return selectNext().submit(function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<List<R>> submit(EntityFunction<IGameLoop, Boolean> filter,
                                                 EntityFunction<IGameLoop, R> function) {

        final List<IGameLoop> gameLoopNewList = new ArrayList<>(gameLoopList);
        final GameLoopFunction<Pair<Boolean, R>> functionInner = gameLoop -> {
            final Boolean k = filter.apply(gameLoop);
            final R v = k ? function.apply(gameLoop) : null;
            return Pair.of(k, v);
        };

        final CompletableFuture<Pair<Boolean, R>>[] futureList = gameLoopNewList.stream()
                .map(gameLoop -> gameLoop.submit(functionInner))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futureList)
                .thenApply(v -> IntStream.range(0, gameLoopNewList.size())
                        .filter(i -> futureList[i].join().getK())
                        .mapToObj(i -> futureList[i].join().getV())
                        .collect(Collectors.toList()));
    }

    @Override
    public <R> CompletableFuture<List<R>> submitAll(EntityFunction<IGameLoop, R> function) {
        return submit(EntityPredicate.True(), function);
    }
}
