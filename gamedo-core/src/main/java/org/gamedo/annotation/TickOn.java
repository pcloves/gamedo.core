package org.gamedo.annotation;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在一个类上，当该类同时是一个spring bean时，该类下的{@link Tick} 方法会被自动注册到{@link #gameLoopId()}指定的线程上，例如：
 * <pre>
 *     &#064;TickOn(gameLoopId = "worker-1")
 *     &#064;Component
 *     public class MyClass
 *     {
 *         &#064;Tick(delay = 0, tick = 50, timeUnit = TimeUnit.MILLISECONDS)
 *         private void tick(Long currentMilliSecond, Long lastMilliSecond)
 *         {
 *             //currentMilliSecond 代表当前系统时间
 *             //lastMilliSecond 代表上一次的心跳时间，如果是第一次调用，该值为-1
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * 该MyClass实例的tick方法会在“worker-1”线程下定时调用
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TickOn {

    /**
     * 要在哪个{@link IGameLoop}下执行心跳函数
     *
     * @return gameLoop的id
     */
    String gameLoopId() default "worker-1";
}
