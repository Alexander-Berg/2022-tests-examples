package ru.yandex.payments.testing.micronaut_cucumber;

import io.cucumber.core.backend.ObjectFactory;
import io.micronaut.aop.InterceptedProxy;
import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.test.annotation.MockBean;
import lombok.val;

public class CucumberObjectFactory implements ObjectFactory {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean addClass(Class<?> glueClass) {
        return true;
    }

    @Override
    public <T> T getInstance(Class<T> glueClass) {
        val context = MicronautCucumberTest.getMicronautContext();
        val instance = context.getBean(glueClass);
        context.findBeanDefinition(glueClass)
                .ifPresent(definition -> alignMocks(context, definition, instance));
        return instance;
    }

    // this logic was stolen from Micronaut Junit 5 extension
    @SuppressWarnings("rawtypes")
    private static <T> void alignMocks(ApplicationContext context, BeanDefinition<T> definition, Object instance) {
        for (val injectedField : definition.getInjectedFields()) {
            val isMock = context.resolveMetadata(injectedField.getType()).isAnnotationPresent(MockBean.class);
            if (isMock) {
                val field = injectedField.getField();
                field.setAccessible(true);
                try {
                    val mock = field.get(instance);
                    if (mock instanceof InterceptedProxy proxy) {
                        val target = proxy.interceptedTarget();
                        field.set(instance, target);
                    }
                } catch (IllegalAccessException e) {
                    // continue
                }
            }
        }
    }
}
