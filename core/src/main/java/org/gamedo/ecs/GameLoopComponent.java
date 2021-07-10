package org.gamedo.ecs;

import org.gamedo.gameloop.interfaces.IGameLoop;

public class GameLoopComponent extends Component<IGameLoop> {

    public GameLoopComponent(IGameLoop owner) {
        super(owner);
    }

}
