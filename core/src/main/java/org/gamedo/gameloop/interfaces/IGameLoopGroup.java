package org.gamedo.gameloop.interfaces;

import org.gamedo.ecs.interfaces.IEntity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * {@link IGameLoopGroup}
 */
public interface IGameLoopGroup extends ExecutorService {

    /**
     * 所属Id
     * @return 实例的Id
     */
    String getId();

    /**
     * 选择所有的线程
     * @return 返回一个数组的副本
     */
    IGameLoop[] selectAll();

    /**
     * 轮询选择下一个线程
     * @return 返回下一个IGameLoop
     */
    IGameLoop selectNext();

    /**
     * 选择一个或多个符合条件的{@link IGameLoop}，首先对所管理的所有{@link IGameLoop}使用选择器：{@link GameLoopFunction#apply(IGameLoop)}，
     * 然后使用比较器：{@link Comparator#compare(Object, Object)}对元素进行排序，最后筛选出前{@code limit}个{@link IGameLoop}，例
     * 如要选择一个{@link IEntity}最少的{@link IGameLoop}：
     * <pre>
     *     gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(), Comparator.reverseOrder(), 1);
     * </pre>
     * 例如对于任意{@link IEntity} id集合：{"a"、"b"、"c"}，如果{@link IGameLoop}含有任意一个{@link IEntity}，则将其选择出来
     * <pre>
     *     final HashSet&#60;String&#62; entityIdSet = new HashSet&#60;&#62;(Arrays.asList("a", "b", "c"));
     *     final List&#60;IGameLoop&#62; gameLoopList = entityIdSet.stream()
     *             .flatMap(s -&#62; gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(s), Comparator.reverseOrder(), 1).stream())
     *             .collect(Collectors.toList());
     * </pre>
     * @param chooser    比较元素抽取器
     * @param comparator 元素比较器
     * @param limit      最大返回的{@link IGameLoop}的个数
     * @param <C>        比较元素的类型
     * @return 返回有效的IGameLoop集合
     */
    <C extends Comparable<? super C>> List<IGameLoop> select(GameLoopFunction<C> chooser,
                                                     Comparator<C> comparator,
                                                     int limit);

    /**
     * 选择一个或多个符合条件的{@link IGameLoop}<p>
     * 例如对于任意{@link IEntity} id集合：{"a"、"b"、"c"}，如果{@link IGameLoop}含有任意一个{@link IEntity}，则将其选择出来
     * <pre>
     *     final HashSet&#60;String&#62; entityIdSet = new HashSet&#60;&#62;(Arrays.asList("a", "b", "c"));
     *     final List&#60;IGameLoop&#62; gameLoopList = gameLoopGroup.select(gameLoop -&#62; gameLoop.getComponent(IGameLoopEntityManager.class)
     *                 .map(iGameLoopEntityManager -&#62; iGameLoopEntityManager.getEntityMap().keySet()
     *                         .stream()
     *                         .anyMatch(s -&#62; entityIdSet.contains(s)))
     *                 .orElse(false)
     *         );
     * </pre>
     * @param filter 过滤器
     * @return 返回有效的IGameLoop集合
     */
    List<IGameLoop> select(GameLoopFunction<Boolean> filter);
}
