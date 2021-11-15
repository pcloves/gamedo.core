package org.gamedo.annotation;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 该注解被标注在一个方法上，代表所归属的类具备在{@link IGameLoop}线程内心跳的能力，含有该注解的方法称为：心跳函数。心跳函数的要求：
 * <ul>
 * <li> 返回值为void，包含2个{@link Long}类型的参数，第1个参数代表当前系统时间，第2个参数代表上次心跳时间（首次心跳时为-1）
 * <li> 某一个类的心跳函数除了包含自己的心跳函数，也包含父类及祖先类内的心跳函数
 * <li> 对于函数重载：假如某函数被子类重载，那么本类或子类只要任意函数上增加了本注解，那么都会成为心跳函数
 * </ul>
 * 使用方式如下：
 * <ul>
 * <li> 定义心跳函数
 * <pre>
 *     class MyTickObject
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
 * <li>将该类的实例注册到某{@link IGameLoop}线程上，如下所示：
 * </ul>
 * <pre>
 * final IGameLoop iGameLoop = ...
 * final MyTickObject myTickObj = new MyTickObject()
 * final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopTickManagerFunction.register(myTickObj))
 * </pre>
 * 当future执行成功后，每隔50毫秒，该tick心跳函数都会被IGameLoop线程调用<p>
 * 有两种情况不需要执行上述的手动注册，系统会自动为其注册：
 * <ul>
 * <li> 某{@link IEntity}被安全发布到{@link IGameLoop}（例如通过:{@link IGameLoopEntityManagerFunction#registerEntity(IEntity)}）
 * 上，那么{@link IEntity}实现类及其父类下所有的{@link Tick}心跳函数会自动注册
 * <li> 当{@link Object}被当做组件通过{@link IEntity#addComponent(Class, Object)}添加到{@link IEntity}上，当{@link IEntity}
 * 被安全发布到{@link IGameLoop}上时，该{@link Object}实现类及其父类下所有的{@link Tick}心跳函数都会自动注册
 * </ul>
 * 当某{@link IEntity}从{@link IGameLoop}反注册后，这两种情况下所有的{@link Tick}心跳函数又会自动被反注册<p>
 * 除此之外，还可以动态注册反注册心跳函数，详情可以参考{@link IGameLoopTickManager}
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
    boolean scheduleWithFixedDelay() default false;
}
