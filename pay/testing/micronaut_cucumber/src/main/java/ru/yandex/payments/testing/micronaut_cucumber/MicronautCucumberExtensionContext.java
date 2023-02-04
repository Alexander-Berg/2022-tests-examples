package ru.yandex.payments.testing.micronaut_cucumber;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;

import static java.util.Collections.emptySet;

class MicronautCucumberExtensionContext implements ExtensionContext {
    private final Class<?> testClass;
    private final Map<ExtensionContext.Namespace, CustomStore> stores;
    @Setter
    private String uniqueId;
    @Setter
    private String scenarioName;

    MicronautCucumberExtensionContext(Class<?> testClass) {
        this.testClass = testClass;
        this.stores = new ConcurrentHashMap<>();
        this.uniqueId = "";
        this.scenarioName = "";
    }

    @Override
    public Optional<ExtensionContext> getParent() {
        return Optional.empty();
    }

    @Override
    public ExtensionContext getRoot() {
        return this;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getDisplayName() {
        return scenarioName;
    }

    @Override
    public Set<String> getTags() {
        return emptySet();
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return Optional.empty();
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        return Optional.of(testClass);
    }

    @Override
    public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
        return Optional.empty();
    }

    @Override
    public Optional<Object> getTestInstance() {
        return Optional.empty();
    }

    @Override
    public Optional<TestInstances> getTestInstances() {
        return Optional.empty();
    }

    @Override
    public Optional<Method> getTestMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getConfigurationParameter(String key) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getConfigurationParameter(String key, Function<String, T> transformer) {
        return Optional.empty();
    }

    @Override
    public void publishReportEntry(Map<String, String> map) {
    }

    @Override
    public ExtensionContext.Store getStore(ExtensionContext.Namespace namespace) {
        return stores.computeIfAbsent(namespace, ignore -> new CustomStore());
    }

    void close() {
        for (val store : stores.values()) {
            store.closeAllCloseableRecords();
        }
    }
}
