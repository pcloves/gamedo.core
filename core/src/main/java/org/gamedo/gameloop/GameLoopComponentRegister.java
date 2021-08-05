package org.gamedo.gameloop;

import lombok.*;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.List;

/**
 * gameLoop组件注册器，{@link IGameLoop}同时作为一个{@link IEntity}，可以挂载任何数量的组件，假如某{@link GameLoopComponent}的实现类
 * 定义如下：
 * <pre>
 * public class GameLoopTest extends GameLoopComponent implements A, B
 * {
 *      //...
 * }
 * </pre>
 * 由于GameLoopTest同时实现了A和B接口，因此可以将GameLoopTest分别以接口A和接口B的身份，增加到{@link IGameLoop}上，例如：
 * <pre>
 *     IGameLoop gameLoop = ...;
 *     GameLoopTest component = new GameLoopTest();
 *     gameLoop.addComponent(A.class, component);
 *     gameLoop.addComponent(B.class, component);
 * </pre>
 * 实际上，上述代码虽然可以正常编译，但是在运行时期间会抛出{@link GameLoopException}异常，这是因为{@link IGameLoop}同时需要确保线程安全，
 * 禁止跨线程对其进行写操作，为了避免这个问题，需要将GameLoopTest线程安全地发布到{@link IGameLoop}实例上，gamedo.core提供了两个方式：
 * <ul>
 * <li> 线程安全地和{@link IGameLoop}进行通信，也即使用{@link IGameLoop#submit(GameLoopFunction)}，那么上述示例可以调整为：
 * <pre>
 *     gameLoop.submit(gameLoop.addComponent(A.class, component));
 *     gameLoop.submit(gameLoop.addComponent(B.class, component));
 * </pre>
 * <li> 在{@link GameLoop}的构造函数{@link GameLoop#GameLoop(GameLoopConfig)}传入{@link GameLoopConfig}，使得在构造期间，将组件
 * 进行注册
 * </ul>
 * 对于第2种方式，{@link GameLoopConfig}内需要配置一个{@link GameLoopComponentRegister}列表，代表{@link IGameLoop}构造时，要注册的
 * 组件，这也正是本类存在的意义
 */
@Builder
@Data
public class GameLoopComponentRegister {
    /**
     * 要注册的组件接口列表，该列表中的Class必须是implementation的实现类或者其父类
     */
    @Singular
    List<Class<?>> allInterfaces;
    /**
     * 组件实现类的类型
     */
    Class<? extends GameLoopComponent> implementation;
}
