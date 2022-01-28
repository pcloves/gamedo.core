package org.gamedo.annotation;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.*;

/**
 * 标注在一个类上，当该类同时是一个spring bean时，该类下的{@link Cron} 方法会被自动注册到{@link #gameLoopId()}指定的线程上，例如：
 * <pre>
 *     &#064;CronOn(gameLoopId = "worker-1")
 *     &#064;Component
 *     public class MyClass
 *     {
 *         &#064;Cron("*&#47;10 * * * * *")
 *         private void cron(Long currentTime, Long lastTriggerTime)
 *         {
 *             //currentTime 当前时间
 *             //lastTriggerTime 代表上一次的运行时间，如果是第一次调用，那么该值为：-1
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * 该MyClass实例的cron方法会在“worker-1”线程下定时调用
 */
@SuppressWarnings("unused")
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CronOn {

    /**
     * 要在哪个{@link IGameLoop}下进行cron调用
     * @return gameLoop的id
     */
    String gameLoopId() default "worker-1";
}
