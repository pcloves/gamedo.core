package org.gamedo.ecs.interfaces;

import org.springframework.context.ApplicationContext;

public interface IApplication extends IEntity {
    ApplicationContext getApplicationContext();
}
