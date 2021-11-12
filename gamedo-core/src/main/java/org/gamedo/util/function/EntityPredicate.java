package org.gamedo.util.function;

import org.gamedo.ecs.interfaces.IEntity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "unchecked"})
@FunctionalInterface
public interface EntityPredicate<T extends IEntity> extends EntityFunction<T, Boolean> {

    EntityPredicate<? extends IEntity> TRUE = entity -> true;
    EntityPredicate<? extends IEntity> FALSE = entity -> false;

    @Override
    Boolean apply(T entity);

    /**
     * 返回一个组合谓词，返回的谓词为本谓词和另一个谓词的短路逻辑：AND。 在组合谓词时，如果此谓词为false ，则不会评估other谓词。
     * 在评估任何谓词期间抛出的任何异常都将转发给调用者；如果对该谓词的求值引发异常，则不会对other谓词求值。
     *
     * @param other 要执行短路逻辑：AND的谓词
     * @return 一个组合谓词，表示这个谓词和other谓词的短路逻辑 AND
     * @throws NullPointerException 如果other为空，则抛出
     */
    default EntityPredicate<T> and(EntityPredicate<T> other) {
        Objects.requireNonNull(other);
        return entity -> apply(entity) && other.apply(entity);
    }

    /**
     * 返回一个组合谓词，返回的谓词为本谓词和另一个谓词的短路逻辑：OR。 在组合谓词时，如果此谓词为true ，则不会评估other谓词。
     * 在评估任何谓词期间抛出的任何异常都将转发给调用者；如果对该谓词的求值引发异常，则不会对other谓词求值。
     *
     * @param other 要执行短路逻辑：OR的谓词
     * @return 一个组合谓词，表示这个谓词和other谓词的短路逻辑：OR
     * @throws NullPointerException 如果other为空，则抛出
     */
    default EntityPredicate<T> or(EntityPredicate<T> other) {
        Objects.requireNonNull(other);
        return entity -> apply(entity) || other.apply(entity);
    }

    /**
     * 返回一个对当前谓词进行逻辑取反的谓词
     *
     * @return 取反后的谓词
     */
    default EntityPredicate<T> negate() {
        return entity -> !apply(entity);
    }

    /**
     * 通过逻辑{@link EntityPredicate#and(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1  第1个参数
     * @param p2  第2个参数
     * @param <P> 谓词的入参类型
     * @return 返回reduce后的谓词，默认返回FALSE
     */
    static <P extends IEntity> EntityPredicate<P> And(final EntityPredicate<P> p1, final EntityPredicate<P> p2) {
        return Stream.of(p1, p2).reduce(EntityPredicate::and).orElse((EntityPredicate<P>) FALSE);
    }

    /**
     * 通过逻辑{@link EntityPredicate#and(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1  第1个参数
     * @param p2  第2个参数
     * @param p3  第3个参数
     * @param <P> 谓词的入参类型
     * @return 返回reduce后的谓词，默认返回FALSE
     */
    static <P extends IEntity> EntityPredicate<P> And(final EntityPredicate<P> p1,
                                                      final EntityPredicate<P> p2,
                                                      final EntityPredicate<P> p3) {
        return Stream.of(p1, p2, p3).reduce(EntityPredicate::and).orElse((EntityPredicate<P>) FALSE);
    }

    /**
     * 通过逻辑{@link EntityPredicate#and(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1 第1个参数
     * @param p2 第2个参数
     * @param p3 第3个参数
     * @param p4 第4个参数
     * @param <P> 实体类型
     * @return 返回reduce后的谓词，默认返回FALSE
     */
    static <P extends IEntity> EntityPredicate<P> And(final EntityPredicate<P> p1,
                                                      final EntityPredicate<P> p2,
                                                      final EntityPredicate<P> p3,
                                                      final EntityPredicate<P> p4) {
        return Stream.of(p1, p2, p3, p4).reduce(EntityPredicate::and).orElse((EntityPredicate<P>) FALSE);
    }

    /**
     * 通过逻辑{@link EntityPredicate#and(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param predicateList 谓词列表
     * @param <P>           谓词的入参类型
     * @return 返回reduce后的谓词，如果列表为空，默认返回FALSE
     */
    static <P extends IEntity> EntityPredicate<P> And(final List<EntityPredicate<P>> predicateList) {
        return predicateList.stream().reduce(EntityPredicate::and).orElse((EntityPredicate<P>) FALSE);
    }

    /**
     * 通过{@link EntityPredicate#or(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1  第1个参数
     * @param p2  第2个参数
     * @param <P> 谓词的入参类型
     * @return 返回reduce后的谓词，默认返回TRUE
     */
    static <P extends IEntity> EntityPredicate<P> Or(final EntityPredicate<P> p1, final EntityPredicate<P> p2) {
        return Stream.of(p1, p2).reduce(EntityPredicate::or).orElse((EntityPredicate<P>) TRUE);
    }

    /**
     * 通过{@link EntityPredicate#or(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1  第1个参数
     * @param p2  第2个参数
     * @param p3  第3个参数
     * @param <P> 谓词的入参类型
     * @return 返回reduce后的谓词，默认返回TRUE
     */
    static <P extends IEntity> EntityPredicate<P> Or(final EntityPredicate<P> p1,
                                                     final EntityPredicate<P> p2,
                                                     final EntityPredicate<P> p3) {
        return Stream.of(p1, p2, p3).reduce(EntityPredicate::or).orElse((EntityPredicate<P>) TRUE);
    }

    /**
     * 通过{@link EntityPredicate#or(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param p1  第1个参数
     * @param p2  第2个参数
     * @param p3  第3个参数
     * @param p4  第4个参数
     * @param <P> 谓词的入参类型
     * @return 返回reduce后的谓词，默认返回TRUE
     */
    static <P extends IEntity> EntityPredicate<P> Or(final EntityPredicate<P> p1,
                                                     final EntityPredicate<P> p2,
                                                     final EntityPredicate<P> p3,
                                                     final EntityPredicate<P> p4) {
        return Stream.of(p1, p2, p3, p4).reduce(EntityPredicate::or).orElse((EntityPredicate<P>) TRUE);
    }

    /**
     * 通过逻辑{@link EntityPredicate#or(EntityPredicate)}的方式将多个{@link EntityPredicate}谓词reduce为1个
     *
     * @param predicateList 谓词列表
     * @param <P>           谓词的入参类型
     * @return 返回reduce后的谓词，如果列表为空，默认返回TRUE
     */
    static <P extends IEntity> EntityPredicate<P> Or(final List<EntityPredicate<P>> predicateList) {
        return predicateList.stream().reduce(EntityPredicate::or).orElse((EntityPredicate<P>) TRUE);
    }

    /**
     * 返回一个永远为true的谓词
     *
     * @param <P> 谓词的入参类型
     * @return 永远为true的谓词
     */
    static <P extends IEntity> EntityPredicate<P> True() {
        return (EntityPredicate<P>) TRUE;
    }

    /**
     * 返回一个永远为false的谓词
     *
     * @param <P> 谓词的入参类型
     * @return 永远为false的谓词
     */
    static <P extends IEntity> EntityPredicate<P> False() {
        return (EntityPredicate<P>) FALSE;
    }

    /**
     * 对一个谓词取反
     *
     * @param target 要取反的谓词
     * @param <P>    谓词的入参类型
     * @return 取反后的谓词
     */
    static <P extends IEntity> EntityPredicate<P> Not(EntityPredicate<? super P> target) {
        Objects.requireNonNull(target);
        return (EntityPredicate<P>) target.negate();
    }
}
