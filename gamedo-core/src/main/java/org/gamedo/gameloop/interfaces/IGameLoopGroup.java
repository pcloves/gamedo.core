package org.gamedo.gameloop.interfaces;

import org.gamedo.util.function.EntityFunction;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.GameLoopFunction;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 每一个{@link IGameLoopGroup}实例代表一个线程组，内部包含若干{@link IGameLoop}实例，{@link IGameLoopGroup}虽然继承了
 * {@link ExecutorService}接口，但是自身只是一个{@link IGameLoop}容器，所有的功能则是由被轮询（round robin）的{@link IGameLoop}提供，
 * 详情可以参考{@link IGameLoop}
 */
public interface IGameLoopGroup extends ExecutorService {

    /**
     * Id
     *
     * @return 实例的Id
     */
    String getId();

    /**
     * 当前管理的{@link IGameLoop}的数量
     *
     * @return 返回值大于等于0
     */
    int size();

    /**
     * 注册一个{@link IGameLoop}，如果注册成功，当调用{@link IGameLoopGroup#selectNext()}后不会立刻被select到，只有在连续调用
     * {@link IGameLoopGroup#selectNext()}，且原有的{@link IGameLoop}都被select完毕后，再次
     * {@link IGameLoopGroup#selectNext()}时，才会被select到新注册的{@link IGameLoop}
     *
     * @param gameLoop 要注册的{@link IGameLoop}
     * @return 如果拥有id重复则返回false，否则返回true，
     */
    boolean register(IGameLoop gameLoop);

    /**
     * 选择指定id的{@link IGameLoop}
     *
     * @param id {@link IGameLoop}的{@link IGameLoop#getId() id}
     * @return 如果不存在，则返回Optional#empty()，否则返回对应的值
     */
    Optional<IGameLoop> select(String id);

    /**
     * 选择所有的{@link IGameLoop}线程
     *
     * @return 返回一个数组的副本
     */
    IGameLoop[] selectAll();

    /**
     * 轮询选择下一个{@link IGameLoop}线程
     *
     * @return 返回下一个IGameLoop
     */
    IGameLoop selectNext();

    /**
     * 选择一个或多个符合条件的{@link IGameLoop}，首先对所管理的所有{@link IGameLoop}使用选择器：
     * {@link GameLoopFunction#apply(IGameLoop)}，
     * 然后使用比较器：{@link Comparator#compare(Object, Object)}对元素进行排序，最后筛选出前{@code limit}个{@link IGameLoop}<p>
     * 例如：选择一个{@link IEntity}最少的{@link IGameLoop}：
     * <pre>
     *     gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(), Comparator.reverseOrder(), 1);
     * </pre>
     * 再例如：对于任意{@link IEntity} id集合：{"a"、"b"、"c"}，如果某{@link IGameLoop}含有集合内任意id的{@link IEntity}，则将其选择
     * 出来：
     * <pre>
     *     final List&#60;CompletableFuture&#60;List&#60;IGameLoop&#62;&#62;&#62; futureList = entityIdSet.stream()
     *                 .map(s -&#62; gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(s),
     *                         Comparator.reverseOrder(),
     *                         1))
     *                 .collect(Collectors.toList());
     *         CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
     *                 .thenApply(r -&#62; futureList.stream()
     *                         .flatMap(t -&#62; t.join().stream())
     *                         .distinct()
     *                         .collect(Collectors.toList()));
     * </pre>
     *
     * @param <C>        比较元素的类型
     * @param chooser    比较元素抽取器（如果同一个抽取器在多处使用，建议申明为静态函数以复用，例如：{@link IGameLoopEventBusFunction}）
     * @param comparator 元素比较器
     * @param limit      最大返回的{@link IGameLoop}的个数
     * @return 返回有效的IGameLoop集合，假如任意线程在执行chooser时抛出了异常，那么该返回CompletableFuture会抛出异常
     */
    <C extends Comparable<? super C>> CompletableFuture<List<IGameLoop>> select(EntityFunction<IGameLoop, C> chooser,
                                                                                Comparator<C> comparator,
                                                                                int limit);

    /**
     * 提交一个操作到轮询的当前{@link IGameLoop}线程
     * @param function 要提交的function
     * @param <R>      提交后的返回值类型
     * @return 操作返回结果
     * @see IGameLoop#submit(EntityFunction)
     */
    <R> CompletableFuture<R> submit(EntityFunction<IGameLoop, R> function);

    /**
     * 向{@link IGameLoopGroup}提交一个function操作，该操作仅在满足filter条件的gameLoop上被执行，该函数可以产生两种特例化变体：
     * <ul>
     * <li> 第1个过滤器参数filter永远为true，此时表示将操作提交到所有{@link IGameLoop}上，也即是{@link IGameLoopGroup#submitAll(EntityFunction)}：
     * <pre>
     *     gameLoopGroup.submit(EntityPredicate.True(), function);
     * </pre>
     * <li> 第2个操作参数直接返回{@link IGameLoop}本身，此时表示筛选符合条件的{@link IGameLoop}：
     * <pre>
     *     gameLoopGroup.submit(someFilter, gameLoop -&#62; gameLoop);
     * </pre>
     * </ul>
     * @param filter 过滤器
     * @param function 要执行的操作
     * @param <R> 返回值类型
     * @return 返回值集合，假如任意线程在submit时抛出了异常，那么该返回CompletableFuture会抛出异常
     */
    <R> CompletableFuture<List<R>> submit(EntityFunction<IGameLoop, Boolean> filter, EntityFunction<IGameLoop, R> function);

    /**
     * 提交一个操作到被本{@link IGameLoopGroup}管理的所有的{@link IGameLoop}上，该方法是
     * {@link IGameLoopGroup#submit(EntityFunction, EntityFunction)}的一个特例，也即：
     * <pre>
     *     gameLoopGroup.submit(EntityPredicate.True(), function);
     * </pre>
     * {@link EntityFunction}的使用方式可以参考{@link IGameLoop#submit(EntityFunction)}，一般性的做法为：
     * <pre>
     *     gameLoopGroup.submitAll(IGameLoopEventBusFunction.post(new EventTest()))
     *                 .exceptionally(throwable -&#62; Collections.emptyList())
     *                 .thenAccept(list -&gt; log.info("{}", list)));
     * </pre>
     *
     * @param function 要提交的操作
     * @param <R>      返回值类型
     * @return 返回值集合，假如任意线程在submit时抛出了异常，那么该返回CompletableFuture会抛出异常
     */
    <R> CompletableFuture<List<R>> submitAll(EntityFunction<IGameLoop, R> function);
}
