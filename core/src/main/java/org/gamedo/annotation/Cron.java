package org.gamedo.annotation;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.functions.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.scheduling.support.CronExpression;

import java.lang.annotation.*;

/**
 * 该注解被标注在一个方法上，代表所归属的类具备了在{@link IGameLoop}线程内根据不同的cron表达式调用自身逻辑的能力，含有该注解的方法称为：cron
 * 函数。cron函数的要求：
 * <ul>
 * <li> 返回值为void，包含2个{@link Long}类型的参数，第1个参数代表当前系统时间，第2个参数代表上次cron调用时间（首次调用时为-1）
 * <li> 某一个类的cron函数除了包含自己的cron函数，也包含父类及祖先类内的cron函数
 * <li> 对于函数重载：假如某函数被子类重载，那么本类或子类只要任意函数上增加了本注解，那么都会成为cron函数
 * </ul>
 * 使用方式如下：
 * <ul>
 * <li> 定义cron函数
 * <pre>
 *     class MySchedule
 *     {
 *         &#064;Scheduled("*&#47;10 * * * * *")
 *         private void cron(Long currentTime, Long lastTriggerTime)
 *         {
 *             //currentTime 当前时间
 *             //lastTriggerTime 代表上一次的运行时间，如果是第一次调用，那么该值为：-1
 *             //执行自己的逻辑
 *         }
 *     }
 * </pre>
 * <li> 将该类的实例注册到{@link IGameLoop}上，如下所示：
 * </ul>
 * <pre>
 * final IGameLoop iGameLoop = ...
 * final MySchedule mySchedule = new MySchedule()
 * final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(ISchedulerFunction.register(mySchedule))
 * </pre>
 * 当future执行成功后，该cron函数都会在IGameLoop线程内按照配置的cron表达式被调用<p>
 * 有两种情况不需要执行上述的手动注册，系统会自动为其注册：
 * <ul>
 * <li> 某{@link IEntity}被安全发布到{@link IGameLoop}（例如通过:{@link IGameLoopEntityManagerFunction#registerEntity(IEntity)}）
 * 上，那么{@link IEntity}实现类及其父类下所有的{@link Cron}函数会自动注册
 * <li> 当{@link Object}被当做组件通过{@link IEntity#addComponent(Class, Object)}添加到{@link IEntity}上后，当{@link IEntity}
 * 被安全发布到{@link IGameLoop}上时，该{@link Object}实现类及其父类下所有的{@link Cron}函数都会自动注册
 * </ul>
 * 当某{@link IEntity}从{@link IGameLoop}反注册后，这两种情况下所有的{@link Cron}函数会被自动反注册<p>
 * 除此之外，还可以动态注册反注册cron函数，详情可以参考{@link IGameLoopScheduler}
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cron {

    /**
     * 以下注释翻译自：{@link CronExpression#parse(String)}<p>
     * spring cron表达式字符串，spring cron表达式是一个使用空格分隔且包含6个时间和日期字段的字符串表达式（类似于unix-based cron，但是有所区别，详情可以查看：
     * <a href=https://stackoverflow.com/questions/30887822/spring-cron-vs-normal-cron>stackoverflow: Spring cron vs normal cron?</a>），
     * 其形式为：
     * <pre>
     * ┌───────────── 秒 (0-59)
     * │ ┌───────────── 分 (0 - 59)
     * │ │ ┌───────────── 时 (0 - 23)
     * │ │ │ ┌───────────── 日 (1 - 31)
     * │ │ │ │ ┌───────────── 月 (1 - 12) (或者使用：JAN-DEC)
     * │ │ │ │ │ ┌───────────── 星期 (0 - 7)(0或7为周日，或者使用：MON-SUN)
     * │ │ │ │ │ │
     * │ │ │ │ │ │
     * * * * * * *
     * </pre>
     * cron表达式遵循以下规则：
     * <ul>
     * <li>字段可以是一个星号（*），代表从头到尾，对于“日”、“星期”字段来说，也可以使用问号（?）取代星号（*）（为了兼容原生cron表达式）
     * <li>范围区间使用短横线（-）连接两个数字，且前后均为闭区间，例如对于秒区间来说：0-30代表0到30秒这个区间
     * <li>范围区间（或者使用*）后面跟着一个“/n”（其中n为数字），指定了该区间内的执行间隔为n，也即每隔n单位触发
     * <li>对于“月”和“星期”字段来说，可以使用其英文名称的前3个字母的缩写（大小写不敏感）
     * <li>对于“日”和“星期”字段来说，可以包含一个“L”(代表：Last)字符，代表着“最后”，对于这两个字段，L又有不同的含义：
     * <ul>
     *     <li>在“日”字段中，L代表月份的最后一天，如果L后面跟的是一个负数（例如：L-n），则代表某月最后一天再往前推n天，如果L后面跟着W（例如：
     *     LW，其中W代表Weekday），则代表月份的最后一个工作日
     *     <li>在“星期”字段中，L代表星期的最后一天，如果L前面有一个数字或者3字母缩写的星期名（例如：nL或者DDDL），则代表某月的最后一个星期n
     * </ul>
     * <li>对于“日”字段来说，可以写成：“nW”，代表：最接近给定日期的工作日，例如：15W，意思是：“最接近该月15日的工作日”。那么，如果15号是
     * 星期六，触发器在14号星期五触发。如果15日是星期天，触发器在16日星期一触发。如果15号是星期二，那么它在15号星期二触发。“1W”，如果这个月的第
     * 一天是星期六，不会跨到上个月，触发器会在这个月的第三天（也就是星期一）触发。只有指定一天（不能是范围或列表）的时候，才能指定“W”字符。
     * <li>对于“星期”字段可以写成为：“d#n”（或者DDD#n），代表每个月的第n个星期d，例如：5#3表示每个月的第3个星期五。
     * </ul>
     * <p>示例：
     * <ul>
     * <li>{@code "0 0 * * * *"} = 每天的整点</li>
     * <li><code>"*&#47;10 * * * * *"</code> = 每隔10秒</li>
     * <li>{@code "0 0 8-10 * * *"} = 每天的8:00、9:00、10:00</li>
     * <li>{@code "0 0 6,19 * * *"} = 每天的6:00 AM和7:00 PM</li>
     * <li>{@code "0 0/30 8-10 * * *"} = 每天的8:00、8:30、9:00、9:30、10:00、10:30</li>
     * <li>{@code "0 0 9-17 * * MON-FRI"} = 工作日的9:00-17:00之间的整点（包括9:00和17:00）</li>
     * <li>{@code "0 0 0 25 12 ?"} = 每个圣诞节的午夜零点整</li>
     * <li>{@code "0 0 0 L * *"} = 每月最后一天的午夜零点整</li>
     * <li>{@code "0 0 0 L-3 * *"} = 每月从3号到最后一天期间的午夜零点</li>
     * <li>{@code "0 0 0 1W * *"} = 每月第一个工作日的午夜零点</li>
     * <li>{@code "0 0 0 LW * *"} = 每月最后一个工作日的午夜零点</li>
     * <li>{@code "0 0 0 * * 5L"} = 每月最后一个星期五的午夜零点</li>
     * <li>{@code "0 0 0 * * THUL"} = 每月最后一个星期六的午夜零点</li>
     * <li>{@code "0 0 0 ? * 5#2"} = 每月第2个星期五的午夜零点</li>
     * <li>{@code "0 0 0 ? * MON#1"} = 每月第1个星期一的午夜零点</li>
     * </ul>
     * <p>可以使用以下快捷的简写方式：
     * <ul>
     * <li>{@code "@yearly"} (或者 {@code "@annually"})每年执行一次，对应于：{@code "0 0 0 1 1 *"}</li>
     * <li>{@code "@monthly"}每月执行一次，对应于：{@code "0 0 0 1 * *"}</li>
     * <li>{@code "@weekly"}每周执行一次，对应于：{@code "0 0 0 * * 0"}</li>
     * <li>{@code "@daily"} (或者{@code "@midnight"})每天执行一次，对应于：{@code "0 0 0 * * *"},</li>
     * <li>{@code "@hourly"}每个小时执行一次，对应于：{@code "0 0 * * * *"}.</li>
     * </ul>
     *
     * @return 一个有效的cron表达式
     * @see <a href=https://tool.lu/crontab/>cron执行时间计算工具（类型请选择：Java(Spring)）</a>
     * @see <a href=https://www.toolnb.com/tools/croncreate.html>Cron表达式生成工具</a>
     * @see <a href=https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling-cron-expression>spring docs: Cron Expressions</a>
     */
    @SuppressWarnings("HtmlTagCanBeJavadocTag") String value() default "";
}
