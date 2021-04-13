package org.gamedo.gameloop.interfaces;

import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.ecs.interfaces.IComponent;

public interface IGameLoopApplicationGroup extends IGameLoopGroup, IComponent {
    IApplication application();
}
