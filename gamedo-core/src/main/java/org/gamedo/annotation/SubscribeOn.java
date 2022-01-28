package org.gamedo.annotation;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在一个类上，当该类同时是一个spring bean时，该类下的{@link Subscribe} 方法会被自动注册到{@link #gameLoopId()}指定的线程上，例如：
 * <pre>
 *     &#064;SubscribeOn(gameLoopId = "worker-1")
 *     &#064;Component
 *     public class MyClass
 *     {
 *         &#064;Subscribe
 *         private void onEventRegisterEntityPost(EventRegisterEntityPost event)
 *         {
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * 该MyClass实例的onEventRegisterEntityPost方法会监听“worker-1”线程下的“EventRegisterEntityPost”事件
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubscribeOn {

    /**
     * 要在哪个{@link IGameLoop}下订阅事件
     *
     * @return gameLoop的id
     */
    String gameLoopId() default "worker-1";
}
