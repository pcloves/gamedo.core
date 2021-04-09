package org.gamedo.gameloop.interfaces;

import org.gamedo.ecs.interfaces.IEntity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface IGameLoopGroup extends ExecutorService, IEntity {
    /**
     * 启动该线程组
     *
     * @param initialDelay   初始延迟
     * @param period         心跳间隔
     * @param periodTimeUnit 心跳间隔的单位
     * @return 返回启动失败的线程列表
     */
    List<IGameLoop> run(long initialDelay, long period, TimeUnit periodTimeUnit);

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
     * 选择一组符合条件的{@link IGameLoop}，首先对{@link IGameLoopGroup}管理的所有{@link IGameLoop}使用抽取器：{@link GameLoopFunction#apply(IGameLoop)}，
     * 将待排序元素抽取出来，然后使用比较器：{@link Comparator#compare(Object, Object)}对元素进行排序，最后筛选出前{@code limit}个{@link IGameLoop}
     *
     * @param chooser    比较元素抽取器
     * @param comparator 元素比较器
     * @param limit      最大返回的{@link IGameLoop}的个数
     * @param <C>        比较元素的类型
     * @return 返回有效的IGameLoop集合
     */
    <C extends Comparable<? super C>> List<IGameLoop> select(GameLoopFunction<C> chooser,
                                                     Comparator<C> comparator,
                                                     int limit);
}
