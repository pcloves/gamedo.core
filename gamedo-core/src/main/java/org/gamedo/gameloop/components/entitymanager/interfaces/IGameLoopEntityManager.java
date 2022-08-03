package org.gamedo.gameloop.components.entitymanager.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.GameLoops;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link IGameLoop}的{@link IEntity 实体}管理组件，负责实体的增加、删除和查询
 */
@SuppressWarnings("unused")
public interface IGameLoopEntityManager extends IComponent<IGameLoop> {

    /**
     * 注册一个实体到本{@link IGameLoopEntityManager 管理器}中，会根据实体的自身分类注册到不同的实例桶中
     *
     * @param <T>    要注册的实体类型
     * @param entity 要注册的实体
     * @return 当且仅当注册成功时返回true，如果实例桶中已经存在一个相同Id的实体，则注册失败
     */
    <T extends IEntity> boolean registerEntity(T entity);

    /**
     * 从管理器中取消一个实体的注册，之后该实体不再归本管理管理
     *
     * @param <T>      要获取的实体类型，上层使用者在调用{@link Optional#get()}并赋值给{@link IEntity}子类时，需要确保合法性，否则会抛出
     *                 {@link ClassCastException}
     * @param entityId 要进行反注册的实体Id
     * @param category 实体分类
     * @return 如果实体不在管理器管理者，则返回{@link Optional#empty()}，否则返回反注册成功的实体{@link Optional}
     */
    <T extends IEntity> Optional<T> unregisterEntity(String entityId, Supplier<String> category);

    /**
     * 检测是否包含一个实体
     *
     * @param entityId 要检测的实体的Id
     * @param category 实体分类
     * @return true表示该实体
     */
    boolean hasEntity(String entityId, Supplier<String> category);

    /**
     * 从管理器中取出一个实体
     *
     * @param <T>      要获取的实体类型，上层使用者在调用{@link Optional#get()}并赋值给{@link IEntity}子类时，需要确保合法性，否则会抛出
     *                 {@link ClassCastException}
     * @param entityId 实体Id
     * @param category 实体分类
     * @return 如果实体不在管理器管理者，则返回{@link Optional#empty()}，否则返回反注册成功的实体{@link Optional}
     */
    <T extends IEntity> Optional<T> getEntity(String entityId, Supplier<String> category);

    /**
     * 返回实体的数量
     *
     * @param category 实体分类
     * @return 实体的数量
     */
    int getEntityCount(Supplier<String> category);

    /**
     * 返回一个无法被修改的{@link Collections#unmodifiableMap(Map) Map}副本
     *
     * @param category 实体分类
     * @return key : value: 实体的Id : 实体本身
     */
    Map<String, IEntity> getEntityMap(Supplier<String> category);


    /**
     * 从当前{@link IGameLoop}线程的{@link IGameLoopEntityManager}组件中获取指定的实体
     *
     * @param entityId 实体id
     * @param category 实体所属分类
     * @param <T>      实体类型
     * @return 当且仅当：1、当前{@link IGameLoop}线程包含{@link IGameLoopEntityManager}组件；2、且包含指定的实体id时，返回该实体id，否则
     * 返回null
     */
    @SuppressWarnings("unchecked")
    static <T extends IEntity> T getEntityById(String entityId, Supplier<String> category) {
        return (T) GameLoops.current()
                .flatMap(gameLoop -> gameLoop.getComponent(IGameLoopEntityManager.class))
                .flatMap(manager -> manager.getEntity(entityId, category))
                .orElse(null);
    }

    /**
     * 从当前{@link IGameLoop}线程的{@link IGameLoopEntityManager}组件中获取指定的实体的指定组件
     *
     * @param entityId      实体id
     * @param category      实体所属分类
     * @param componentType 组件所属的Class
     * @param <T>           实体类型
     * @param <C>           组件类型
     * @return 当且仅当：1、当前{@link IGameLoop}线程包含{@link IGameLoopEntityManager}组件；2、{@link IGameLoopEntityManager}包含指定的实体id；
     * 3、实体包含指定的组件时，返回该组件，否则返回null
     */
    static <T extends IEntity, C> C getComponentByEntityId(String entityId, Supplier<String> category, Class<C> componentType) {
        return GameLoops.current()
                .flatMap(gameLoop -> gameLoop.getComponent(IGameLoopEntityManager.class))
                .flatMap(manager -> manager.getEntity(entityId, category))
                .flatMap(entity -> ((IEntity) entity).getComponent(componentType))
                .orElse(null);
    }
}
