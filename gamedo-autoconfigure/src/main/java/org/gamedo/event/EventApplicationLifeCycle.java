package org.gamedo.event;

import lombok.Value;
import org.gamedo.GameLoopPostProcessor;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

/**
 * 应用程序生命周期事件，所有的{@link IGameLoop}都可以监听到如下有效{@link Stage}的事件，且当{@link IGameLoop}线程处理该事件时，事件
 * 源头线程会被阻塞住，但是最多会等待5分钟（在{@link GameLoopPostProcessor#onApplicationEvent(ApplicationEvent)}内写死了）
 */
@Value
public class EventApplicationLifeCycle implements IEvent {

    Stage stage;

    @SuppressWarnings("unused")
    public enum Stage {
        /**
         * 未知
         */
        Unknown(null),
        /**
         * 该事件的源头是main主线程，此时应用程序已经启动完毕，但是{@link ApplicationRunner}和{@link CommandLineRunner}还没有调用
         */
        Started(ApplicationStartedEvent.class),
        /**
         * 该事件的源头是main主线程，此时应用程序已经就绪
         */
        Ready(ApplicationReadyEvent.class),
        /**
         * 该事件的源头是jvm的shutdown hook线程，应用程序可以收到该事件后执行清理工作
         */
        Shutdown(ContextClosedEvent.class),
        ;

        public final Class<? extends ApplicationEvent> applicationEventClass;

        Stage(Class<? extends ApplicationEvent> applicationEventClass) {
            this.applicationEventClass = applicationEventClass;
        }

        public static Stage of(Class<? extends ApplicationEvent> applicationEventClass) {
            for (Stage stage : Stage.values()) {
                if (stage.applicationEventClass == applicationEventClass) {
                    return stage;
                }
            }

            return Unknown;
        }
    }
}
