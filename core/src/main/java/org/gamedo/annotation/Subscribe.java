package org.gamedo.annotation;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBusFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解被标注在一个方法上，代表所归属的类具备在{@link IGameLoop}线程内进行事件处理的能力，一般性的做法为：
 * <ul>
 * <li>定义需要事件处理的类，并定义只含有{@link IEvent}类型参数的方法（我们称之为事件处理方法），每个类内事件处理方法的数量不受限制
 * <li>声明一个{@link IEvent}子类，并且将所有的成员变量声明为final，并且我们建议使用lombok的{@link lombok.Value}注解（gamedo的后续版本
 * 可能会对成员变量是否都为final进行检测，不符合条件的事件将不会被抛到{@link IGameLoopEventBus}）上
 * <li>在方法上增加本注解
 * 示例如下：
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
 * </ul>
 * <pre>
 * final IGameLoop iGameLoop = ...
 * final MyObject myObj = new MyObject()
 * final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopEventBusFunction.register(myObj))
 * </pre>
 * 之后，当有事件EventRegisterEntityPost被抛出到IGameLoop时，MyObject.onEventRegisterEntityPost就会触发（该回调是发生在最初注册的
 * IGameLoop线程上）<p>
 * <b>需要注意：</b>当myObj被当做组件附加到某{@link IEntity} A上后，就不需要手动执行上述注册代码了，系统会在A注册到{@link IGameLoop}
 * 上时，自动注册A本身以及所有组件内的事件处理方法，并且当从{@link IGameLoop}反注册后又会自动反注册这些事件处理方法<p>
 * 对于跨线程的消息通信，可以使用{@link IGameLoopEventBusFunction#post(IEvent)}提供的线程安全的方式，将{@link IEvent}从{@link IGameLoop}
 * A发布到{@link IGameLoop} B，这也是为什么要求{@link IEvent}的成员变量都定义为final类型，通过final关键字，保证了线程间的内存可见性
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
}
