package ru.yandex.payments.testing.micronaut_cucumber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.extension.ExtensionContext;

class CustomStore implements ExtensionContext.Store {
    private final Map<Object, Object> store = new ConcurrentHashMap<>();

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T castToRequiredTypeOrNull(@Nullable Object value, @NonNull Class<T> requiredType) {
        if (value != null && requiredType.isAssignableFrom(value.getClass())) {
            return (T) value;
        } else {
            return null;
        }
    }

    @Override
    public <V> V get(Object key, Class<V> requiredType) {
        val value = get(key);
        return castToRequiredTypeOrNull(value, requiredType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Object getOrComputeIfAbsent(K key, Function<K, V> defaultCreator) {
        return store.computeIfAbsent(key, k -> defaultCreator.apply((K) k));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getOrComputeIfAbsent(K key, Function<K, V> defaultCreator, Class<V> requiredType) {
        val value = store.computeIfAbsent(key, k -> defaultCreator.apply((K) k));
        return castToRequiredTypeOrNull(value, requiredType);
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return store.remove(key);
    }

    @Override
    public <V> V remove(Object key, Class<V> requiredType) {
        val value = store.remove(key);
        return castToRequiredTypeOrNull(value, requiredType);
    }

    @SneakyThrows
    void closeAllCloseableRecords() {
        for (val value : store.values()) {
            if (value instanceof CloseableResource resource) {
                resource.close();
            }
        }
    }
}
