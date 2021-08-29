package org.gamedo.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Entity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.util.Pair;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.junit.jupiter.api.Test;

@Log4j2
class ICacheManagerTest {
    private static final String EntityId = "entityId";
    private final ICacheManager cacheManager = new CacheManager();
    private final IGameLoop gameLoop = new GameLoop("gameLoop");

    ICacheManagerTest() {
        gameLoop.submit(gameLoop -> gameLoop.addComponent(IGameLoopEventBus.class, new GameLoopEventBus(gameLoop))).join();
        gameLoop.submit(gameLoop -> gameLoop.addComponent(IGameLoopEntityManager.class, new GameLoopEntityManager(gameLoop))).join();
        gameLoop.submit(IGameLoopEventBusFunction.register(cacheManager)).join();

        final Entity entity = new Entity(EntityId);
        final ComponentDbDataBag component = new ComponentDbDataBag(100, 200);
        component.setId(entity.getId());

        entity.addComponent(ComponentDbDataBag.class, component);

        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity)).join();
    }

    @Test
    void test() {

        final Pair<String, Class<ComponentDbDataBag>> key = Pair.of(EntityId, ComponentDbDataBag.class);
        final AsyncLoadingCache<Pair<String, Class<ComponentDbDataBag>>, ComponentDbDataBag> cache = cacheManager.getCache(key);
        final ComponentDbDataBag componentDbDataBag = cache.get(key).join();

        log.info("{}", componentDbDataBag);
    }

}