package ru.yandex.qe.cache;

import javax.annotation.Nullable;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 05.07.13
 * Time: 21:04
 */
@Component
public class AnnotatedTestStorage extends BaseTestStorage {
    @Nullable
    @Override
    @Cacheable(value = "annotatedCache")
    public Object getData(int id) {
        return keyValue.get(id);
    }

    @Override
    @CacheEvict(value = "annotatedCache", allEntries = true)
    public void put(int id, Object object) {
        keyValue.put(id, object);
    }
}
