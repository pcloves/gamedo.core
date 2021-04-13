package org.gamedo.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(GamedoConfiguration.class)
@Documented
public @interface EnableGamedoApplication {
}
