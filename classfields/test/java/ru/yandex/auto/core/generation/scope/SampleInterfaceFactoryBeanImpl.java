package ru.yandex.auto.core.generation.scope;

import org.springframework.beans.factory.FactoryBean;

/**
 * User: yan1984
 * Date: 10.05.2011 19:56:23
 */
public class SampleInterfaceFactoryBeanImpl implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        return new SampleInterfaceImpl();
    }

    @Override
    public Class getObjectType() {
        return SampleInterface.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
