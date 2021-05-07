package org.gamedo.application;

import lombok.Value;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;

import java.util.List;

/**
 * 在game.core框架下，当一个{@link Object}实现了接口：{@link IEntity}，那么就表明它是一个实体，而任意的{@link Object}都可以作为一个组件
 * 被添加到{@link IEntity}上，代表该{@link IEntity}具备了被添加组件的的能力。在game.core框架中，该思想可以说是框架的核心思想之一，例如负责
 * 线程管理的{@link GameLoop}，作为game.core的用户，也理应该延续该思想。{@link GamedoApplication}也是{@link IEntity}的一个实现类（同时
 * 它还是{@link IApplication}的实现类），并且它具有一个很大的特殊性：它的对象只有一个。而这个独一无二的对象，可以拥有特性丰富的组件，例如可以为
 * 它添加若干：{@link IGameLoopGroup}，对于使用者，对于某些具有全局特性的类，将其唯一对象作为组件添加到{@link GamedoApplication}中也是一
 * 个不错的选择，例如全服活动管理器、全服好友管理器，而这些组件应该在应用程序启动后正式提供服务前被成功注册，这也是本类的一个主要功能
 * 当应用程序启动后，game.core会扫描所有的类型为：{@link ApplicationComponentRegister}的bean，并且对于每一个bean，执行如下操作：
 * 遍历{@link ApplicationComponentRegister#clazzList}，依次调用{@link IEntity#addComponent(Class, Object)}，将其注册为
 * {@link GamedoApplication}的组件，并且可以每个组件都可以注册1个或多个{@link Class}，当调用{@link IEntity#getComponent(Class)}时，
 * 可以根据需要传入指定的{@link Class}
 * <p>
 * 需要注意的是：当某个{@link Object}被作为组件添加到{@link IEntity}上时，需要指定{@link Object}的一个父类或接口的{@link Class}作为唯一
 * 标识，这意味着：同一个{@link IEntity}实体不能两个完全相同的组件，例如假如有组件A定义如下：
 * <pre>
 *     public class A implements X
 *     {
 *     }
 * </pre>
 * 当执行如下逻辑时：
 * <pre>
 *     X a1 = new A();
 *     X a2 = new A()
 *     entity.addComponent(X.class, a1);
 *     entity.addComponent(X.class, a2);
 * </pre>
 * 会导致entity内只有一个类型为X的组件，并且为a2，因为添加a2时，会导致a1被覆盖，解决这个冲突的思路为，定义一个新的接口Y使之extends X，然后添加
 * a2组件时，使用Y.class，如下所示：
 * <pre>
 *     public interface Y extends X
 *     {
 *
 *     }
 *
 *     entity.addComponent(Y.class, a2);
 * </pre>
 * 对于{@link GamedoApplication}而言，面对同样的情景，因此在定义{@link ApplicationComponentRegister}类型bean时，其添加的组件类型：
 * {@link ApplicationComponentRegister#clazzList}就不能重复，例如添加多个{@link IGameLoopGroup}时，就要使用上述的方案绕过该限制
 * @param <T> 组件的实现类型
 */
@Value
public class ApplicationComponentRegister<T> {
    /**
     * 代表要注册的{@link Class}列表，当调用{@link IEntity#addComponent(Class, Object)}时，第一个参数代表待添加组件的类型，由于每个组
     * 件可能有接口实现或者祖先类，因此此处使用列表，
     */
    List<Class<? super T>> clazzList;
    /**
     * 代表组件本身，当然此处的T可以是组件的真正实现类，也可以是组件的接口类或祖先类，这完全取决于用户
     */
    T object;
}
