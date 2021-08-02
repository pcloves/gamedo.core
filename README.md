![GitHub](https://img.shields.io/github/license/pcloves/gamedo.core?style=flat-square) ![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/pcloves/gamedo.core?style=flat-square) ![GitHub Workflow Status](https://img.shields.io/github/workflow/status/pcloves/gamedo.core/Java%20CI%20with%20Maven?style=flat-square)

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

这是IGameLoop作为ScheduledExecutorService的实现，所带来理所应当的特性，同时IGameLoop作为[ECS](https://en.wikipedia.org/wiki/Entity_component_system) 中的IEntity接口，也提供了组件（component）管理的功能，当将这两者结合，就带来了令人欣喜的线程模型：

``` java
@SuppressWarnings("ALL")
@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        final String id = "gamedo";
        final EventGreeting event = new EventGreeting("hello " + id);
        final IGameLoop worker = Gamedo.worker().selectNext();

        //模拟使用io线程加载entity
        CompletableFuture.supplyAsync(() -> loadEntity(id), Gamedo.io())
                //加载完毕，将之安全发布到worker线程
                .thenAccept(entity -> worker.submit(IGameLoopEntityManagerFunction.registerEntity(entity)))
                //注册完毕，向其发送消息
                .thenAccept(s -> worker.submit(IGameLoopEventBusFunction.post(event)));
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

        //每隔60秒执行一次心跳，可以声明任意数量的@Tick函数
        @Tick(delay = 0, tick = 60, timeUnit = TimeUnit.SECONDS)
        private void tick(Long currentMilliSecond, Long lastMilliSecond) {
            log.info("ticking...");
        }
      
        //每天凌晨执行跨天调用，可以声明任意数量的@Cron函数
        @Cron("@daily")
        private void cron(Long currentTime, Long lastTriggerTime) {
            log.info("it's a new day.");
        }

        //响应EventGreeting事件，可以声明任意数量的@Subscribe函数
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

执行完毕后，输出日志如下：

```
2021-08-03 23:59:58.961  INFO 59376 --- [    main] com.example.demo.Application             : application start...
2021-08-03 23:59:58.962  INFO 59376 --- [    io-1] com.example.demo.Application             : load entity from db, entity:Entity{hashCode=-1253235656, id=gamedo, componentMap=[MyComponent]}
2021-08-03 23:59:58.977  INFO 59376 --- [worker-1] com.example.demo.Application             : receive greeting:hello gamedo
2021-08-03 23:59:58.978  INFO 59376 --- [worker-1] com.example.demo.Application             : ticking...
2021-08-04 00:00:00.007  INFO 59376 --- [worker-1] com.example.demo.Application             : it's a new day.
2021-08-04 00:00:58.981  INFO 59376 --- [worker-1] com.example.demo.Application             : ticking...
2021-08-04 00:01:58.990  INFO 59376 --- [worker-1] com.example.demo.Application             : ticking..
```

根据以上代码和日志可以得出如下分析：

* entity实体是从io线程加载的

* entity实体被**安全发布**到worker-1线程
* entity的MyComponent组件跟随entity被注册到worker线程，由于注册了@Tick、@Cron、@Subscribe注解，因此自动具备了3种能力：
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

# 简述

## 定义

* **IEntity** IEntity代表[ECS](https://en.wikipedia.org/wiki/Entity_component_system) 中E，代表了每一个IEntity实例都具备**组合**任意组件（Component）的能力。在gamedo.core中，可以通过通过向某IEntity增加组件，扩展IEntity的能力。
* **IGameLoop** IGameLoop作为ScheduledExecutorService的实现，具备了后者所有的特性。同时，每一个IGameLoop实例的**coreSize**为1，代表内部**仅**维护了一个独立线程。
* **IGameLoopGroup** IGameLoopGroup管理了一组IGameLoop，类似于netty 4中的EventLoopGroup，虽然也继承了ScheduledExecutorService接口，但是其能力完全依赖于所管理的IGameLoop，IGameLoopGroup是真正意义上的线程池。

## 线程模型

gamedo.core的线程模型借鉴了netty的线程模型：每一个IGameLoop实例代表一个线程，对应于Netty的EventLoop，而IGameLoopGroup则对应于Netty的EventLoopGroup，正如Netty的EventLoopGroup一样，IGameLoopGroup虽然继承了ExecutorService接口，但是自身只是一个IGameLoop容器，所有的功能则是通过轮询（round robin）的IGameLoop提供，在实际的应用中，每个IEntity实例应该被唯一的安全发布（safe publication）到某个IGameLoop（安全发布是《JCP》一书中关于线程安全话题的重要概念，详情可以查阅本书），这和Netty中任意Channel的整个生命周期都隶属于某一个线程（EventLoop）的理念也是一致的（这也是相对于Netty 3，Netty 4为什么能获得巨大性能提升的重要原因之一）。

IGameLoop作为IEntity和ScheduledExecutorService的扩展，同时具备了两者的能力：异步（延迟、周期）执行任务和组件管理，除此之外，IGameLoop还提供了一个线程安全的，可以与之通信的能力：IGameLoop#submit(GameLoopFunction)，和ScheduledExecutorService不同的是：当提交线程不在本线程中时，任务被异步执行；当提交线程就在本线程内时，任务会同步立即执行，此时调用者可以通过CompletableFuture#getNow(Object)立刻得到结果。

IGameLoop#submit(GameLoopFunction)提供了<b>线程安全</b>的，由<b>外部世界发起的</b>，将<b>任意数据类型X</b>提交到IGameLoop线程，并且可以返回<b>任意类型Y</b>的双向通信能力。借助于IGameLoop#submit(GameLoopFunction)，可以进一步使用IGameLoop的内置组件：IGameLoopEventBus，将事件发布到IGameLoop，单向地与之通讯，例如：

``` java
    final IGameLoop iGameLoop = ...
    final IEvent event = new SomeEvent();
    final CompletableFuture<Integer> future = iGameLoop.submit(IGameLoopEventBusFunction.post(event))
```

gamedo.core的starter工程利用spring boot的自动装配功能（autoconfigure），为IGameLoop自动装配了若干必备且开箱即用的组件，
包括：

* **IGameLoopEntityManager** 提供线程内的IEntity管理机制
* **IGameLoopEventBus** 提供线程内的事件动态订阅、发布、处理机制
* **IGameLoopScheduler** 提供线程内的cron动态管理机制
* **IGameLoopTickManager** 提供线程内的逻辑心跳的动态管理机制

当某个IEntity实例被安全发布到IGameLoop上时，该实例及其所有组件都具备了**事件订阅**、**cron延迟运行**、**逻辑心跳**的能力，详情可以参考org.gamedo.annotation包内关于@Subscribe、@Cron以及@Tick的注释。这些组件的使用方式可以参考org.gamedo.gameloop.functions包内提供的IGameLoop*Function函数或者单元测试。

由于IGameLoop自身也是一个IEntity，因此理所当然可以被自己的**IGameLoopEntityManager **组件管理。因此在gamedo.core的默认实现中，当IGameLoop被实例化之后，也会通过IGameLoopEntityManagerFunction#registerEntity(IEntity)函数注册自己，使得IGameLoop自身及其组件也具备**事件订阅**、**cron延迟运行**、**逻辑心跳**的基础能力，实例代码如下：

``` java
final GameLoop gameLoop = new GameLoop(config);
gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop));
```

## 配置

gamedo.core默认内置了3种线程池：

* **worker** cpu密集型线程池，该线程池类似于RxJava的Schedulers.computation()或Reactor的Schedulers.parallel()，默认线程数量为：Runtime.getRuntime().availableProcessors() + 1
* **io** io密集型线程池，该线程池类似于RxJava的Scheduler.io()或Reactor的Schedulers.boundedElastic() ，默认线程数量为：Runtime.getRuntime().availableProcessors() * 10，在实际应用中，这个值应该是根据分析或者监控工具进行指标检测，然后根据公式计算得出，在
  《JCP》一书中，建议通过估算任务等待时间和计算时间的比值，来估算io密集型的线程数量，并给出了确切的计算方案。
* **single** 唯一线程的线程池，某些并发业务场景需要操作强一致性（例如经典的抢票行为），对于这种需求，可以将所有请求提交到本线程池，通过将并行请求串行化， 解决日常场景中的并发需求

可以在application.yml中对上述线程池进行调整，例如可以通过如下配置调整io线程：

``` yaml
gamedo:
  gameloop:
    ios:
      game-loop-id-prefix: test #线程名称前缀
      game-loop-count: 1 #线程池内线程数量
      game-loop-group-id: tests #线程池的名称
      game-loop-id-counter: 1 #线程名称起始值
      component-registers: #IGameLoop要挂载的组件
        - all-interfaces:
            - org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager
          implementation: org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager
        - all-interfaces:
            - org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus
          implementation: org.gamedo.gameloop.components.eventbus.GameLoopEventBus
      daemon: true #是否为后台线程池
```

在实际使用中，可能会对IGameLoop的组件进行扩展，除了使用上述配置中的：component-registers，也可以在代码中动态注册组件。

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