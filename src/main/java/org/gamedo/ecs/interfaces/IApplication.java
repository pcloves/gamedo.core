package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

public interface IApplication extends IEntity {
    ApplicationContext getApplicationContext();

    /**
     * 启动所有类型为{@link IGameLoopGroup}的组件的{@link IGameLoopGroup#run(long, long, TimeUnit)}
     * 这个接口还得想一想，有点太武断，因为不一定每一个{@link IGameLoopGroup}都要运行，最好控制权下放到他们自己身上，或者开放配置
     *
     * @return 返回的线程数量
     */
    int runAllGameLoopGroupComponent();
}
