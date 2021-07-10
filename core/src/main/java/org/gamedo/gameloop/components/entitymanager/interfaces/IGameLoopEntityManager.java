package org.gamedo.gameloop.components.entitymanager.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Map;
import java.util.Optional;

/**
 * {@link IGameLoop}的{@link IEntity 实体}管理组件，负责实体的增加、删除和查询
 */
public interface IGameLoopEntityManager extends IComponent<IGameLoop> {

    /**
     * 注册一个实体到本{@link IGameLoopEntityManager 管理器}中
     *
     * @param entity 要注册的实体
     * @return 当且仅当注册成功时返回true，如果已经存在一个相同Id的实体，则注册失败
     */
    boolean registerEntity(IEntity entity);

    /**
     * 从管理器中取消一个实体的注册，之后该实体不再归本管理管理
     * @param entityId 要进行反注册的实体Id
     * @return 如果实体不在管理器管理者，则返回{@link Optional#empty()}，否则返回反注册成功的实体{@link Optional}
     */
    Optional<IEntity> unregisterEntity(String entityId);

    /**
     * 检测是否包含一个实体
     * @param entityId 要检测的实体的Id
     * @return true表示该实体
     */
    boolean hasEntity(String entityId);

    /**
     * 返回实体的数量
     * @return 实体的数量
     */
    int getEntityCount();

    /**
     * 返回一个无法被修改的{@link java.util.Collections#unmodifiableMap(Map) Map}副本
     * @return key : value: 实体的Id : 实体本身
     */
    Map<String, IEntity> getEntityMap();
}
