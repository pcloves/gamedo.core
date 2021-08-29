package org.gamedo.cache;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gamedo.annotation.Subscribe;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.event.EventRegisterEntityPost;
import org.gamedo.gameloop.components.eventbus.event.EventUnregisterEntityPost;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.persistence.db.ComponentDbData;
import org.gamedo.util.Pair;
import org.gamedo.util.function.EntityFunction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@SuppressWarnings({"unused", "unchecked", "rawtypes"})
@Log4j2
public class CacheManager implements ICacheManager {
    private final ConcurrentMap<IGameLoop, AsyncLoadingCache> cacheMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IGameLoop> entityId2GameLoopMap = new ConcurrentHashMap<>();

    @Subscribe
    private void eventRegisterEntityPost(EventRegisterEntityPost event) {
        entityId2GameLoopMap.put(event.getEntityId(), event.getGameLoop());
        log.warn("EventRegisterEntityPost, event:{}", event);
    }

    @Subscribe
    private void eventUnregisterEntityPost(EventUnregisterEntityPost event) {
        entityId2GameLoopMap.remove(event.getEntityId());
        log.warn("EventUnregisterEntityPost, event:{}", event);
    }

    @Override
    public <V extends ComponentDbData> AsyncLoadingCache<Pair<String, Class<V>>, V> getCache(Pair<String, Class<V>> key) {

        final String entityId = key.getK();

        final IGameLoop iGameLoop = entityId2GameLoopMap.get(entityId);
        if (iGameLoop == null) {
            //从db加载？
            return null;
        }

        return cacheMap.computeIfAbsent(iGameLoop, iGameLoop1 -> {
            return Caffeine.newBuilder()
                    .executor(iGameLoop1)
                    .maximumSize(10000)
                    .<Pair<String, Class<V>>, V>buildAsync(getAsyncCacheLoader());
        });
    }

    private static <V extends ComponentDbData> AsyncCacheLoader<Pair<String, Class<V>>, V> getAsyncCacheLoader() {

        return (@NonNull Pair<String, Class<V>> key, @NonNull Executor executor) -> {
            final String entityId = key.getK();
            final Class<V> componentDbDataClazz = key.getV();
            final IGameLoop gameLoop = (IGameLoop) executor;
            final EntityFunction<IGameLoop, V> function = iGameLoop -> {
                return iGameLoop.getComponent(IGameLoopEntityManager.class)
                        .flatMap(iGameLoopEntityManager -> iGameLoopEntityManager.getEntity(entityId))
                        .flatMap(iEntity -> iEntity.getComponent(componentDbDataClazz))
                        .orElse(null);
            };

            return gameLoop.submit(function);
        };
    }
}
