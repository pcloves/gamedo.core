package org.gamedo.annotation;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * 该注解被标注在一个方法上，代表所归属的类具备了在{@link IGameLoop}线程内订阅事件、响应事件的能力，含有该注解的方法称为：handle函数。
 * handle函数的要求：
 * <ul>
 * <li> 返回值为void，包含1个{@link IEvent}子类参数
 * <li> 某一个类的handle函数除了包含自己的handle函数，也包含父类及祖先类内的handle函数
 * <li> 对于函数重载：假如某函数被子类重载，那么本类或子类只要任意函数上增加了本注解，那么都会成为handle函数
 * 使用方式如下：
 * </ul>
 * <ul>
 * <li> 定义handle函数
 * <pre>
 *     class MyObject
 *     {
 *         &#064;Subscribe
 *         private void onEventRegisterEntityPost(EventRegisterEntityPost event)
 *         {
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * <li>将该类的实例注册到{@link IGameLoop}上，如下所示：
 * <pre>
 * final IGameLoop iGameLoop = ...
 * final MyObject myObj = new MyObject()
 * final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopEventBusFunction.register(myObj))
 * </pre>
 * </ul>
 * 之后，当有事件EventRegisterEntityPost被抛到iGameLoop线程上，MyObject.onEventRegisterEntityPost就会被调用，且调用线程就在
 * iGameLoop中<p>
 * 有两种情况不需要执行上述的手动注册，系统会自动为其注册：
 * <ul>
 * <li> 某{@link IEntity}被安全发布到{@link IGameLoop}（例如通过:{@link IGameLoopEntityManagerFunction#registerEntity(IEntity)}）
 * 上，那么{@link IEntity}实现类及其父类下所有的{@link Subscribe}函数会自动注册
 * <li> 当{@link Object}被当做组件通过{@link IEntity#addComponent(Class, Object)}添加到{@link IEntity}上后，当{@link IEntity}
 * 被安全发布到{@link IGameLoop}上时，该{@link Object}实现类及其父类下所有的{@link Subscribe}函数都会自动注册
 * </ul>
 * {@link IGameLoopEventBusFunction#post(Class, Supplier)}提供了线程安全的事件安全策略，可以将事件安全发布到任意{@link IGameLoop}线程，出于多
 * 线程并发中的内存可见性的考虑，我们强烈建议{@link IEvent}的成员变量都定义为final类型（可以使用lombok的{@link lombok.Value}注解），并且
 * 绝不允许将已经注册到某{@link IGameLoop}线程的{@link IEntity}及其组件跨线程发布到其他的{@link IGameLoop}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * 代表处理事件的优先级，值越小，代表优先级越高，{@link Short#MIN_VALUE}代表最高优先级
     * @return 优先级
     */
    short value() default 0;
}
