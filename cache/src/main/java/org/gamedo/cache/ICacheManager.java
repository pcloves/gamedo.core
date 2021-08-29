package org.gamedo.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.gamedo.persistence.db.ComponentDbData;
import org.gamedo.util.Pair;

public interface ICacheManager {
    /**
     * 获取一个异步缓存
     * @param key entityId和DbData的class类型共同组建后的key
     * @param <V> 数据类型
     * @return 返回一个缓存
     */
    <V extends ComponentDbData> AsyncLoadingCache<Pair<String, Class<V>>, V> getCache(Pair<String, Class<V>> key);
}
