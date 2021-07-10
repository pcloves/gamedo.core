package org.gamedo.annotation;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 该注解被标注在一个方法上，代表所归属的类具备在{@link IGameLoop}线程内进行心跳的能力，一般性的使用方法：
 * <ul>
 * <li>创建需要进行心跳的类，并定义包含两个Long类型参数的方法（我们称之为心跳方法），每个类内心跳方法的数量不受限制
 * <li>在方法上增加本注解
 * 示例如下：
 * <pre>
 *     class MyTickObject
 *     {
 *         &#064;Tick(delay = 100, tick = 50, timeUnit = TimeUnit.MILLISECONDS)
 *         private void tick(Long currentMilliSecond, Long lastMilliSecond)
 *         {
 *             //currentMilliSecond 代表当前系统时间
 *             //lastMilliSecond 代表上一次的心跳时间，如果是第一次调用，该值为-1
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * <li>将该类的实例注册到{@link IGameLoop}上，如下所示：
 * </ul>
 * <pre>
 * final IGameLoop iGameLoop = ...
 * final MyTickObject myTickObj = new MyTickObject()
 * final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopTickManagerFunction.register(myTickObj))
 * </pre>
 * <b>需要注意：</b>当MyTickObject被当做组件附加到某{@link IEntity} A上后，就不需要执行上述注册代码了，系统会在A注册到{@link IGameLoop}
 * 上时，自动注册A本身以及所有组件的心跳函数，并且当从{@link IGameLoop}反注册后又会自动反注册这些心跳函数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tick {

    /**
     * @return 心跳的延迟开启时间
     */
    long delay() default 0L;

    /**
     * @return tick心跳间隔
     */
    long tick() default 50L;

    /**
     * @return tick心跳时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * @return true：以scheduleWithFixedDelay方式执行，false：以scheduleAtFixedRate方式执行
     */
    boolean scheduleWithFixedDelay() default true;
}
