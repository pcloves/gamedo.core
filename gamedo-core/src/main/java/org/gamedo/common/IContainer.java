package org.gamedo.common;

@SuppressWarnings("unused")
public interface IContainer<T extends IContainerKey> {
    /**
     * 往容器中添加一个数据
     *
     * @param key  该数据对应的key
     * @param data 数据本身
     * @return 如果要添加的数据的类型是key的要求的类型或其子类，则添加成功并返回该容器本身，否则返回null
     */
    IContainer<T> put(T key, Object data);

    /**
     * 从容器中获取一个数据
     *
     * @param key 要获取的可以
     * @param <V> 期待获取的数据类型
     * @return 如果容器中包含指定的key，并且数据是key要求的类型或其子类，则返回该数据，否则返回null
     */
    <V> V get(T key);
}
