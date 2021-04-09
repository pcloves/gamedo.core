package org.gamedo.scheduling;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CronScheduled {

    /**
     * spring cron表达式字符串，spring cron表达式是一个使用空格分隔且包含6个时间、日期字段的字符串表达式（类似于unix-based cron，但是有所区别，详情可以查看：
     * <a href=https://stackoverflow.com/questions/30887822/spring-cron-vs-normal-cron>stackoverflow: Spring cron vs normal cron?</a>），
     * 其形式为：
     * <p>
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
     * <p>
     * cron表达式遵循以下规则：
     * <ul>
     * <li>字段可以是一个星号（*），代表从头到尾，对于“日”、“星期”字段来说，问号可能会取代星号
     * <li>范围区间使用短横线（-）连接两个数字，且前后均为闭区间，例如对于秒区间来说：0-30代表0到30秒这个区间
     * <li>范围区间或（或者*）后面跟着一个“/n”（其中n为数字），则指定了该区间内的间隔为n
     * <li>对于“月”和“星期”字段来说，可以使用其英文名称的前3个字母的缩写（大小写不敏感）
     * <li>对于“日”和“星期”字段来说，可以包含一个“L”(代表：Last)字符，代表着“最后”，并且L对于这两个字段，又有不同的含义：
     * <ul>
     *     <li>在“日”字段中，L代表月的最后一天，如果L后面跟的是一个负数（例如：L-n），则代表从n开始到月末，如果后面跟着W（例如：LW，其中W代表
     *     Weekday），则代表：某月的最后一个工作日
     *     <li>在“星期”字段中，L代表星期的最后一天，如果L前面有一个数字或者3字母缩写的星期名（例如：nL或者DDDL），则代表某月的最后一个星期n
     * </ul>
     * <li>对于“日”字段来说，可以写成：“nW”，代表：最接近给定日期的工作日，例如：15W，意思是：“最接近该月15日的工作日。”。所以，如果15号是
     * 星期六，触发器在14号星期五触发。如果15日是星期天，触发器在16日星期一触发。如果15号是星期二，那么它在15号星期二触发。“1W”，如果这个月的第
     * 一天是星期六，不会跨到上个月，触发器会在这个月的第三天（也就是星期一）触发。只有指定一天（不能是范围或列表）的时候，才能指定“W”字符。
     * <li>对于“星期”字段可以写成为：“d#n”（或者DDD#n），代表每个月的第n个星期d，例如：表示每个月的第三个星期五。
     * </ul>
     * <p>示例：
     * <ul>
     * <li>{@code "0 0 * * * *"} = 每一天的整点</li>
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
     * @see <a href=https://tool.lu/crontab/>cron执行时间计算工具（类型请选择：Java(Spring)）</a>
     * @see <a href=https://www.toolnb.com/tools/croncreate.html>Cron表达式生成工具</a>
     * @see <a href=https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling-cron-expression>spring docs: Cron Expressions</a>
     * @return 一个有效的cron表达式
     */
    @SuppressWarnings("HtmlTagCanBeJavadocTag") String value() default "";
}
