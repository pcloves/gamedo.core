![GitHub](https://img.shields.io/github/license/pcloves/gamedo.core?style=flat-square)![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/pcloves/gamedo.core?style=flat-square)![GitHub Workflow Status](https://img.shields.io/github/workflow/status/pcloves/gamedo.core/Java%20CI%20with%20Maven?style=flat-square)

gamedo.core是gamedo游戏服务器框架的核心模块（正在开发中-进度：76%）

# 快速使用

当使用spring-boot项目后，可以非常方便地使用gamedo.core项目，以下是gamedo.core线程池的开箱使用演示：

``` java
@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        final IGameLoop worker = Gamedo.worker().selectNext();

        CompletableFuture.runAsync(() -> log.info("I'm in a worker thread."), worker)
                .thenAcceptAsync(s -> log.info("then i came to some io thread"), Gamedo.io())
                .thenAcceptAsync(s -> log.info("then i came back to the worker thread."), worker);
    }
}
```

这是IGameLoop作为ScheduledExecutorService的实现，所带来理所应当的特性，同时IGameLoop作为[ECS](https://en.wikipedia.org/wiki/Entity_component_system)中的IEntity接口，也提供了组件（component）管理的功能，当将这两者结合，就带来了令人欣喜的线程模型：

``` java
@SuppressWarnings("ALL")
@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        final String id = "gamedo";
        //假设使用io线程加载entity
        final CompletableFuture<IEntity> future = CompletableFuture.supplyAsync(() -> loadEntity(id), Gamedo.io());

        //加载完毕，将之安全发布到worker线程
        future.thenAccept(entity -> Gamedo.worker().submit(IGameLoopEntityManagerFunction.registerEntity(entity)))
                //从所有的worker线程中，选择曾经注册过该entity的线程
                .thenAccept(s -> Gamedo.worker()
                        .select(IGameLoopEntityManagerFunction.hasEntity(id))
                        .thenAccept(list -> list.forEach(gameLoop -> gameLoop.submit(IGameLoopEventBusFunction.post(new EventGreeting("hello " + id))))));
    }

    private static IEntity loadEntity(String id) {

        IEntity entity = new Entity(id);
        entity.addComponent(MyComponent.class, new MyComponent(entity));

        log.info("load entity from db, entity:{}", entity);
        return entity;
    }

    @Value
    public static class EventGreeting implements IEvent {
        String content;
    }

    @Value
    public static class EventResponse implements IEvent {
        String response;
    }

    public static class MyComponent extends EntityComponent {
        public MyComponent(IEntity owner) {
            super(owner);
        }

        @Tick(delay = 0, tick = 60, timeUnit = TimeUnit.SECONDS)
        private void tick(Long currentMilliSecond, Long lastMilliSecond) {
            log.info("ticking...");
        }

        @Cron("@daily")
        private void cron(Long currentTime, Long lastTriggerTime) {
            log.info("it's a new day.");
        }

        @Subscribe
        private void eventHello(EventGreeting event) {
            log.info("receive greeting:{}", event.content);

            //响应整个“世界”(也就是当前线程的主人手下的所有线程了)
            final EventResponse response = new EventResponse("hello world!");
            GameLoops.current().flatMap(gameLoop -> gameLoop.owner())
                    .ifPresent(gameLoopGroup -> gameLoopGroup.submitAll(IGameLoopEventBusFunction.post(response)));
        }
    }
}
```

对于以上代码代表了gamedo.core线程模型的大概使用方式，当main函数执行完毕后，意味着如下事实发生：

* entity实体是从io线程加载的

* entity实体被**安全发布**到某个worker线程，从此在该线程生存
* entity的MyComponent组件跟随entity被注册到worker线程，并且由于@Tick、@Cron、@Subscribe注解的原因，自动具备了3种能力：
  * 在worker线程内逻辑心跳（tick函数）
  * 延迟执行（cron函数）
  * 响应所有发布到worker线程的事件（eventGreeting函数）

# maven配置

gamedo.core暂时还未发布正式版，目前只能使用snapshots版本，配置如下：

``` xml
<dependency>
  <groupId>org.gamedo</groupId>
  <artifactId>gamedo-spring-boot-starter</artifactId>
  <version>${version}-SNAPSHOT</version>
</dependency>

<repository>
  <id>oss.sonatype.org-snapshot</id>
  <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

# 后续工作

- [ ] slf4j MDC最佳实践落地
  - [ ] log4j2.xml增加mdc字段
  - [ ] ScheduledExecutorService在beforeExecutor和afterExecutor时，设置mdc字段，例如IEntity.getId
- [ ] 指标采集
  - [ ] 每个线程的entity管理的Gauges统计
  - [ ] @Cron执行Timer统计
  - [ ] @Subscribe执行Timer采集
  - [ ] @Tick执行Timer采集
  - [ ] IGameLoop（ScheduledExecutorService）线程池指标采集
- [ ] 指标可视化：开箱即用的通用grafana dashboard id？
- [ ] 持久化继承：考虑将gamedo.persistence集成到starter项目？
- [ ] IGameLoop持续改进
  - [ ] 自定义RejectedExecutionHandler设置
  - [ ] jvm shutdown钩子注册，如何优雅退出？