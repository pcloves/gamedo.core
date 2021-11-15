package org.gamedo.ecs.interfaces;

import org.gamedo.ecs.Component;
import org.gamedo.ecs.EntityComponent;

/**
 * 实体的组件接口，当实现类实现该接口，意味着：
 * <ul>
 * <li> 其实现类具备身份id识别的能力（来自：{@link IIdentity}），默认身份来自于所归属实体的id
 * <li> 其实现类具备了查询其他接口实现的能力（来自：{@link IInterfaceQueryable}）
 * <li> 其实现类具备了设置实体归属的能力
 * </ul>
 * 在gamedo.core框架下，{@link IEntity}实体仅仅是一个组件容器，而组件才是真正的逻辑承载者。从{@link IEntity}实体提供的接口函数来看，
 * 虽然其组合的组件可以是任意类型的{@link Object}，而在实际的开发中，建议使用默认实现类：{@link Component}或者{@link EntityComponent}
 * 或者这两者的子类作为父类，并在此基础上扩展业务接口。以玩家的背包组件为例，从业务逻辑的角度上讲，需要定义业务逻辑相关的背包接口，例如：
 * <pre>
 * public interface IComPlayerBag
 * {
 *      boolean hasItem(int itemId);
 * }
 * </pre>
 * 背包组件的实现类继承自{@link EntityComponent}（或者定义一个业务组件基类，例如：MyGameEntityComponent extends EntityComponent，
 * 所有的业务逻辑组件都继承自该组件类，从而实现更统一地组件管理），并且实现背包接口：
 * <pre>
 * public class ComPlayerBag extends EntityComponent implements IComPlayerBag {
 *      protected ComPlayerBag(IEntity owner, EntityDbPlayer entityDbPlayer) {
 *         super(owner);
 *     }
 *
 *     &#64;Override
 *     public boolean hasItem(int itemId) {
 *         return true;
 *     }
 * }
 * </pre>
 * @param <T> 所归属实体的类型
 */
public interface IComponent<T extends IEntity> extends IInterfaceQueryable, IIdentity {

    /**
     * 获取该组件所归属的实体
     * @return 返回所属实体
     */
    T getOwner();

    /**
     * 设置组件归属的实体，实现类需要首先检测{@code owner}下是否已经有该组件，如果尚未
     * @param owner 要归属的新的实体
     * @return 如果设置成功返回true，如果
     */
    boolean setOwner(T owner);

    /**
     * 默认返回所属实体的唯一id
     * @return 返回所属实体的身份id
     */
    @Override
    default String getId() {
        return getOwner() != null ? getOwner().getId() : "";
    }
}
