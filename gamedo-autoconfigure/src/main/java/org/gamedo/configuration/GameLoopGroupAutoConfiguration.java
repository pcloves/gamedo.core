package org.gamedo.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.gamedo.ApplicationBase;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.util.GamedoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GamedoProperties.class, GameLoopProperties.class, MetricProperties.class})
public class GameLoopGroupAutoConfiguration {

    private final ApplicationContext context;
    private final GamedoProperties gamedoProperties;
    private final GameLoopProperties gameLoopProperties;
    private final MetricProperties metricProperties;

    public GameLoopGroupAutoConfiguration(ApplicationContext context,
                                          GamedoProperties gamedoProperties,
                                          GameLoopProperties gameLoopProperties,
                                          MetricProperties metricProperties) {
        this.context = context;
        this.gamedoProperties = gamedoProperties;
        this.gameLoopProperties = gameLoopProperties;
        this.metricProperties = metricProperties;

        updateSystemProperty();
    }

    private void updateSystemProperty() {
        System.setProperty(GamedoConfiguration.MAX_EVENT_POST_DEPTH_KEY,
                String.valueOf(gameLoopProperties.getMaxEventPostDepth()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_ENTITY_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isEntityEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_EVENT_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isEventEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_CRON_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isCronEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_TICK_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isTickEnable()));
    }

    /**
     * 默认的线程池配置
     *
     * @return 默认的线程池配置，服务于{@link #gameLoop(GameLoopConfig)}和{@link #gameLoopGroup(GameLoopConfig)}，
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfig() {
        return gameLoopProperties.getDefaults().convert();
    }

    /**
     * 非单例bean，每次从spring容器中获取线程时，返回全新的线程，且配置源为：{@link #gameLoopConfig()}，外部也可以传入自定义的
     * {@link GameLoopConfig}线程配置给spring容器，获得自定义的线程
     *
     * @param config 线程配置，如果调用{@link ApplicationContext#getBean(Class)}，那么返回的线程的配置源自{@link #gameLoopConfig()}，
     *               如果调用{@link ApplicationContext#getBean(Class, Object...)}，那么需要自定义线程配置
     * @return 返回一个全新的线程
     */
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoop gameLoop(GameLoopConfig config) throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {

        final Set<String> disabledGameLoopGroup = metricProperties.getDisabledGameLoopGroup();
        final String[] beanNamesForType = context.getBeanNamesForType(MeterRegistry.class);
        final String gameLoopGroupId = config.getGameLoopGroupId();
        final boolean metricEnable = metricProperties.isEnable() && !disabledGameLoopGroup.contains(gameLoopGroupId) && beanNamesForType.length > 0;
        final Class<? extends IGameLoop> gameLoopClazz = config.getGameLoopImplClazz();
        final Constructor<? extends IGameLoop> constructorMetric = gameLoopClazz.getConstructor(GameLoopConfig.class, MeterRegistry.class);
        final Constructor<? extends IGameLoop> constructorNoMetric = gameLoopClazz.getConstructor(GameLoopConfig.class);

        return metricEnable ? constructorMetric.newInstance(config, context.getBean(MeterRegistry.class)) : constructorNoMetric.newInstance(config);
    }

    /**
     * 非单例bean，每次从spring容器中获取线程池时，返回全新的线程池，且配置源为：{@link #gameLoopConfig()}，外部也可以传入自定义的
     * {@link GameLoopConfig}线程池配置给spring容器，获得自定义的线程池
     *
     * @param config 线程池配置，如果调用{@link ApplicationContext#getBean(Class)}，那么返回的线程池的配置源自{@link #gameLoopConfig()}，
     *               如果调用{@link ApplicationContext#getBean(Class, Object...)}，那么需要自定义线程池配置
     * @return 每次返回一个全新的线程池，不建议使用相同的线程池配置调用多次
     */
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoopGroup gameLoopGroup(GameLoopConfig config) {
        return ApplicationBase.createGameLoopGroup(config, context);
    }

    @Bean(name = "configWorker")
    @ConditionalOnMissingBean(name = "configWorker")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfigWorker() {
        return gameLoopProperties.getWorker().convert();
    }

    @Bean(name = "worker")
    @ConditionalOnMissingBean(name = "worker")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    IGameLoopGroup gameLoopGroupWorker(@Qualifier("configWorker") GameLoopConfig config) {
        return ApplicationBase.createGameLoopGroup(config, context);
    }

    @Bean(name = "configSingle")
    @ConditionalOnMissingBean(name = "configSingle")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfigSingle() {
        return gameLoopProperties.getSingle().convert();
    }

    @Bean(name = "single")
    @ConditionalOnMissingBean(name = "single")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    IGameLoopGroup gameLoopGroupSingle(@Qualifier("configSingle") GameLoopConfig config) {
        return ApplicationBase.createGameLoopGroup(config, context);
    }

}
