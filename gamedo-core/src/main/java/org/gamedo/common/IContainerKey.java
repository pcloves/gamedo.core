package org.gamedo.common;

import java.util.function.Supplier;

/**
 * 容器的key
 */
public interface IContainerKey extends Supplier<String> {
    /**
     * 所属类型
     */
    Class<?> getType();

    /**
     * 该key的默认名字
     *
     * @return 该key对应的类型的简要名称
     */
    @Override
    default String get() {
        return this.getType().getSimpleName();
    }
}
