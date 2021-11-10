package org.gamedo.ecs.interfaces;

/**
 * 身份表示接口
 */
@FunctionalInterface
public interface IIdentity {
    /**
     * 身份Id
     *
     * @return 唯一Id
     */
    String getId();
}
