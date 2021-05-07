package org.gamedo.gameloop;

import lombok.Value;
import org.gamedo.application.ApplicationComponentRegister;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.List;

/**
 * 本类类似于{@link ApplicationComponentRegister}，实现相同的功能，区别在于：
 * <ul>
 * <li> 本类是作为{@link IGameLoop}的组件注册器，而{@link ApplicationComponentRegister}是作为{@link IApplication}的组件注册器
 * <li> 本类的bean绝大部分情况下都应该不是单例的，也即应该为：{@link ConfigurableBeanFactory#SCOPE_PROTOTYPE}，这是如下几个原因：
 * <ul>
 * <li> 对于每一个{@link IGameLoopGroup}，都可以拥有一个或多个{@link IGameLoop}，况且{@link IGameLoopGroup}也不是单例的，因此
 * {@link IGameLoop}的组件理应该不是单例的
 * <li> 每一个{@link IGameLoop}代表一个线程，如果某一个单例的{@link Object}被注册到多个{@link IGameLoop}中，那么势必涉及到多线程的问题，
 * 而在gamedo.core的设计哲学里，{@link IGameLoop}的组件都应该是不用关心线程安全问题的，只需要将重点放在自身业务逻辑上
 * <li> 如果本bean是单例的，那么应该作为组件使用{@link ApplicationComponentRegister}注册到{@link IApplication}上
 * </ul>
 * <li> 本类的bean方法需要接受一个参数，类型为：{@link IGameLoop}，而{@link ApplicationComponentRegister}的bean方法无参的
 * </ul>
 * 使用示例可以参考：{@link GamedoConfiguration#gameLoopEventBusRegister(IGameLoop)}
 * @param <T> 组件的类型
 */
@Value
public class GameLoopComponentRegister<T> {
    List<Class<? super T>> clazzList;
    T object;
}
