package org.gamedo.logging;


import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * 日志记录最佳实践（1）：<p>
 * 当一个复杂系统由众多模块组成或者由团队多人开发时，输出的日志会逐渐偏向于混乱和无序，遇到如下场景时，就会愈加明显：
 * <ul>
 * <li> 在开发开发一个新的功能或模块时，只想关注某一个模块或几个模块日志，如何做到？
 * <li> 生产环境下，当某模块或功能出现bug时，如何通过线上日志快速定位定位bug？
 * <li> 性能测试时，如何统计日志输出top 10？如何更精确化收集日志指标？
 * <li> 在对日志进行配置时，想对某些模块的日志进行定制化，例如DB日志单独输出到特定文件、某些异常日志需要告警，如何做到？
 * </ul>
 * 这些示例都说明：如果我们想更加精确地操控输出日志，那么就需要对日志进行更更细粒度的分类，log4j2提供了{@link Marker}（slf4j、logback也提供
 * 了该特性），这个{@link Marker}类似于tag标记，可以利用它对日志进行过滤、分类等定制化操作。而想要使用{@link Marker}的特性，需要配置文件和代
 * 码两方面协同配合。<p>
 * 以将整个系统的日志进行模块化分类为例，完整示例如下：
 * <ul>
 * <li> 首先进行对日志进行配置
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Configuration status="debug">
 *     <Appenders>
 *         <Console name="Console" target="SYSTEM_OUT">
 *             <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %p [$${event:Marker:-Unknown}] %m%n"/>
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
 * <li> 上面的配置中，最重要的地方在于PatternLayout.pattern中的：[$${event:Marker:-Unknown}]，当日志含有{@link Marker}输出时，此处
 * 输出{@link Marker#getName()}，否则默认输出为：Unknown。
 * <li> 当我们记录日志时，可以使用包含{@link Marker}参数的方式进行输出：
 * <pre>
 * {@code
 * log.debug(Markers.GameLoopEntityManager, "register begin, entityId:{}", () -> entityId);
 * }
 * </pre>
 * <li> 同时，仍然可以使用普通方式记录日志：
 * <pre>
 * {@code
 * log.info("GameLoop bean, id:{}", id);
 * }
 * </pre>
 * <li> 日志的输出为：
 * <pre>
 * 2021-07-19 20:26:59.265 INFO [Unknown] GameLoop bean, id:GameLoopGroup1-9
 * 2021-07-19 20:27:00.067 DEBUG [gamedo.entityManager] register begin, entityId:45a5d7fe-8bac-44f1-872f-14b2c42caa53
 * </pre>
 * </ul>
 * 通过日志输出可以发现，前者输出中增加了模块名称，开发环境下可以使用IDEA的grep插件进行过滤，生产环境下可以使用grep命令进行过滤。在linux下，使
 * 用bash命令就能快速高效的对日志top 10进行统计。<p>
 * 对于上面提到的第4个问题，log4j2官方手册中给出了{@link Marker}的使用场景，可以参考
 * <a href="https://logging.apache.org/log4j/2.x/manual/markers.html">官方文档</a>
 * @see GamedoLogContext
 */
@SuppressWarnings("unused")
public final class Markers
{
    public static final Marker GamedoCore = of("gamedo.core");
    public static final Marker GamedoMetrics = of("gamedo.metrics", GamedoCore);
    public static final Marker GameLoop = of("gamedo.gameLoop", GamedoCore);
    public static final Marker GameLoopEntityManager = of("gamedo.entityManager", GameLoop);
    public static final Marker GameLoopEventBus = of("gamedo.eventBus", GameLoop);
    public static final Marker GameLoopTickManager = of("gamedo.tickManager", GameLoop);
    public static final Marker GameLoopScheduler = of("gamedo.scheduler", GameLoop);
    public static final Marker GameLoopTest = of("gamedo.test", GameLoop);
    public static final Marker GameLoopContainer = of("gamedo.container", GameLoop);

    private Markers() {
    }

    public static Marker of(String name, Marker ... parents) {
        return MarkerManager.getMarker(name).setParents(parents);
    }
}
