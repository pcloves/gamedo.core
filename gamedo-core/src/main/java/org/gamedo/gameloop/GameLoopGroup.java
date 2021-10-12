package org.gamedo.gameloop;

import lombok.extern.log4j.Log4j2;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
public class GameLoopGroup implements IGameLoopGroup {
    private final String id;
    private final AtomicInteger idx = new AtomicInteger(0);
    private final List<IGameLoop> gameLoopList = new CopyOnWriteArrayList<>();

    public GameLoopGroup(String id, IGameLoop... gameLoops) {

        if (gameLoops.length == 0) {
            log.error(Markers.GameLoop, "none gameLoop setted for:{}", id);
            throw new GameLoopException("");
        }

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
        gameLoopList.addAll(Arrays.stream(gameLoops).collect(Collectors.toList()));
    }

    public GameLoopGroup(String id, int gameLoopCount) {
        this(id, IntStream.rangeClosed(1, gameLoopCount)
                .mapToObj(value -> new GameLoop(id + '-' + value))
                .toArray(GameLoop[]::new));
    }

    @Override
    public void shutdown() {
        gameLoopList.forEach(gameLoop -> gameLoop.shutdown());
    }

    @Override
    public List<Runnable> shutdownNow() {

        return gameLoopList.stream()
                .map(gameLoop -> gameLoop.shutdownNow())
                .flatMap(runnables -> runnables.stream())
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

        if (gameLoopList.contains(gameLoop)) {
            return false;
        }

        final int size = gameLoopList.size();
        //计算位置：当前位置的前一个位置，也就是说轮询一圈后才能被select到
        final int indexAdd = Math.abs((idx.get() + size) % (size + 1));
        gameLoopList.add(indexAdd, gameLoop);
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
    public IGameLoop selectHashing(Object object2Hash) {
        return gameLoopList.get(Math.abs(object2Hash.hashCode() % gameLoopList.size()));
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
