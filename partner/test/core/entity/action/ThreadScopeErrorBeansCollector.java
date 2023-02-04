package ru.yandex.partner.core.entity.action;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;

import ru.yandex.partner.core.action.ActionUserIdContext;
import ru.yandex.partner.core.configuration.annotation.ThreadScope;

@TestConfiguration
public class ThreadScopeErrorBeansCollector implements MergedBeanDefinitionPostProcessor {
    public static final Set<Class<?>> THREAD_LOCAL_CLASSES = Set.of(
            ActionUserIdContext.class
    );

    private final Set<String> brokenBeans = new HashSet<>();
    private final Set<String> goodBeans = new HashSet<>();

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (THREAD_LOCAL_CLASSES.stream().noneMatch(it -> it.isAssignableFrom(beanType))) {
            return;
        }

        if (beanType.isAnnotationPresent(ThreadScope.class)) {
            goodBeans.add(beanName);
            return;
        }

        Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
        if (factoryMethod != null && factoryMethod.isAnnotationPresent(ThreadScope.class)) {
            goodBeans.add(beanName);
            return;
        }

        brokenBeans.add(beanName);
    }

    public Set<String> getBrokenBeans() {
        return brokenBeans;
    }

    public Set<String> getGoodBeans() {
        return goodBeans;
    }
}
