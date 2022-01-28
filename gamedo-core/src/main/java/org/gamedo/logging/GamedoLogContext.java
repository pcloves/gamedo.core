package org.gamedo.logging;

import org.apache.logging.log4j.Marker;
import org.gamedo.annotation.Cron;
import org.gamedo.annotation.Subscribe;
import org.gamedo.annotation.Tick;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.io.Closeable;
import java.util.Stack;

/**
 * 日志记录最佳实践（2）：<p>
 * 通过对{@link Marker}的使用，记录的日志被细分为不同的模块，可以进行快速的筛选，然而在实际团队开发中，存在更细化的需求场景：
 * <ul>
 * <li> 在开发开发一个新的功能或模块时，如何做到只专注某一个IEntity的日志？
 * <li> 生产环境下，当某模块或功能出现bug时，如何通过线上日志快速筛选定位某个IEntity（玩家、NPC）的日志？
 * </ul>
 * 一般性的做法是：每个功能开发者在记录日志时，额外记录IEntity信息，这和web开发下记录tracId是一个道理，而这种方式带来的缺点也是明确的：由于规范
 * 不统一，这为之后排查日志带来很大的困扰。<p>
 * gamedo.core利用log4j2的<a href=https://logging.apache.org/log4j/2.x/manual/lookups.html>Lookups</a>特性，实现了自定义
 * Plugin：{@link GamedoLookup}，当log4j2请求的Looksup名为：“gamedo:entityId”时，返回当前线程下的维护entityId堆栈的栈顶元素。上层逻
 * 辑可以在适当的逻辑点将entityId push到该堆栈中，并在适当位置从堆栈pop出来（以下称这种操作为埋点），那么在这段时间内，log4j2都可以获取到该
 * entityId。因此可以在日志中记录上该entity的id，gamedo.core已经在框架层对所有的逻辑点进行埋点，框架用户（几乎）不需要额外的处理，这些逻辑点
 * 包括：
 * <ul>
 * <li> 当{@link IGameLoop}内任意task执行前（此时堆栈为空），push {@link IGameLoop#getId()}到堆栈栈顶，当执行后clear堆栈
 * <li> 当{@link Subscribe} handle函数被调用时，对于一个事件处理函数，依次执行：push消费者的id -&#62; 调用handle函数 -&#62; pop消费
 * 者id
 * <li> 当{@link Tick} 心跳函数被调用时，会依次执行：push心跳对象的id -&#62; 调用心跳函数 -&#62; pop心跳对象的id
 * <li> 当{@link Cron} cron函数被调用时，会依次执行：push cron对象的id -&#62; 调用cron函数 -&#62; pop cron对象的id
 * </ul>
 * 假如log4j2的配置为：
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Configuration status="debug">
 *     <Appenders>
 *         <Console name="Console" target="SYSTEM_OUT">
 *             <PatternLayout pattern="%d{ISO8601} [%10.10thread] %-5level [$${event:Marker:-unknown}] [$${gamedo:entityId}] %message%n"/>
 *         </Console>
 *     </Appenders>
 *     <Loggers>
 *         <Root level="TRACE">
 *             <AppenderRef ref="Console" />
 *         </Root>
 *     </Loggers>
 * </Configuration>
 * }
 * </pre>
 * 可以得到如下日志：
 * <pre>
 * {@code
 * 2021-08-05T19:34:05,014 [ default-5] INFO  [unknown] [d9b576dc-c254-430a-ae5f-e45e0a1d0cf0] MyScheduleComponent, lastTriggerTime:1628163244008, thread:default-5
 * 2021-08-05T19:34:05,014 [ default-5] INFO  [unknown] [d9b576dc-c254-430a-ae5f-e45e0a1d0cf0] MyScheduleComponent, lastTriggerTime:1628163244008, thread:default-
 * }
 * </pre>
 * 其中“d9b576dc-c254-430a-ae5f-e45e0a1d0cf0”为entityId<p>
 * 当上述情景不满足需求时，用户可以自定义push和pop，实现更细粒度的日志记录<p>
 * {@link GamedoLogContext}中包含默认的{@link IEntityIdConverter}实现，实现了基本的Object到唯一Id的转换，用户可以通过
 * {@link GamedoLogContext#setEntityIdConverter(IEntityIdConverter)}实现id转换的自定义设置。
 * @see Markers
 */
@SuppressWarnings("unused")
public final class GamedoLogContext {

    public static final ThreadLocal<Stack<String>> ENTITY_ID_STACK = ThreadLocal.withInitial(Stack::new);
    public static volatile IEntityIdConverter converter = new IEntityIdConverter() {
    };

    private GamedoLogContext() {
    }

    /**
     * 自定义id转换器
     *
     * @param converter 新的id转换器
     */
    static void setEntityIdConverter(IEntityIdConverter converter) {
        GamedoLogContext.converter = converter;
    }

    /**
     * 将object的唯一id push到栈顶
     *
     * @param object 要push的object
     */
    public static void pushEntityId(Object object) {
        ENTITY_ID_STACK.get().push(converter.convert(object));
    }

    /**
     * push一个自定义id到栈顶
     *
     * @param id 要push的id
     */
    public static void pushEntityId(String id) {
        ENTITY_ID_STACK.get().push(id);
    }

    /**
     * 返回一个closeable资源，用于new之后自动入栈，close时自动出栈
     * @param object 要push的object
     * @return 返回可以close的资源
     */
    public static CloseableEntityId pushEntityIdAuto(Object object) {
        return new CloseableEntityId(object);
    }

    /**
     * 返回一个closeable资源，new之后自动入栈，close时自动出栈
     * @param id 要push的id
     * @return 返回可以close的资源
     */
    public static CloseableEntityId pushEntityIdAuto(String id) {
        return new CloseableEntityId(id);
    }

    /**
     * pop栈顶的id
     *
     * @return 如果当前栈为空，返回null，否则返回pop后的id
     */
    public static String popEntityId() {
        final String pop;
        try {
            pop = ENTITY_ID_STACK.get().pop();
        } catch (Exception e) {
            throw new GameLoopException("entity id stack is empty!", e);
        }

        return pop;
    }

    public static void clearEntityId() {
        ENTITY_ID_STACK.get().clear();
    }

    /**
     * 自动入栈出栈的id资源
     */
    public static class CloseableEntityId implements Closeable {
        public CloseableEntityId(Object object) {
            pushEntityId(converter.convert(object));
        }

        public CloseableEntityId(String id) {
            pushEntityId(id);
        }

        @Override
        public void close() {
            popEntityId();
        }
    }
}
