package org.gamedo.ecs.interfaces;

import java.util.Optional;

public interface IInterfaceQueryable {

    /**
     * 获取一个接口，等价的伪代码为：
     * <pre>
     *     if(this instanceof clazz) {
     *         return Optional.of(this);
     *     }
     *     else {
     *         return Optional.empty();
     *     }
     * </pre>
     *
     * @param clazz 要获取的接口
     * @param <T>   要获取的接口类型
     * @return 如果待检测实例是class类及其子类，则强制
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<T> getInterface(Class<T> clazz) {
        return Optional.ofNullable(clazz != null && clazz.isInstance(this) ? (T) this : null);
    }
}
