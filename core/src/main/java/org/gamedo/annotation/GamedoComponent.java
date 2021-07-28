package org.gamedo.annotation;

import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解被标注在{@link GameLoopComponent}的子类上，代表这是一个{@link IGameLoop}的组件，且可以通过spring容器创建
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface GamedoComponent {
}
