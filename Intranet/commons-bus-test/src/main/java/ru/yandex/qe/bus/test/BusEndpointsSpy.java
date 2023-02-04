package ru.yandex.qe.bus.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NamedBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import ru.yandex.qe.bus.factories.BusEndpointBuilder;
import ru.yandex.qe.bus.factories.BusServerBuilderBean;
import ru.yandex.qe.bus.factories.client.BusClientBuilderBean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

public class BusEndpointsSpy implements BeanPostProcessor {

    private static final Bus DEFAULT_SPY_BUS = spy(new ExtensionManagerBus());

    private final List<Object> mockServerServices = new ArrayList<>();
    private Bus spyBus = DEFAULT_SPY_BUS;

    public <T> T getMockServerService(Class<T> serviceClass) {
        for (Object spiedService : mockServerServices) {
            if (serviceClass.isAssignableFrom(spiedService.getClass())) {
                //noinspection unchecked
                return (T) spiedService;
            }
        }
        throw new IllegalArgumentException(String.format("spied server service of type %s not found", serviceClass.getName()));
    }

    public void resetMocks() {
        for (Object spyServerService : mockServerServices) {
            reset(spyServerService);
        }
        reset(spyBus);
    }

    public Bus getSpyBus() {
        return spyBus;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        try {
            if (bean instanceof BusEndpointBuilder) {
                replaceTransportOnLocal(bean);
            }
            if (bean instanceof BusClientBuilderBean) {
                setDirectDispatch(bean);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new BeanCreationException(beanName, "Can't spy bus endpoint", e);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            if (bean instanceof BusEndpointBuilder) {
                spyBus(bean);
            }
            if (bean instanceof BusServerBuilderBean) {
                mockServer(bean);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new BeanCreationException(beanName, "Can't spy bus endpoint", e);
        }
        return bean;
    }

    private void replaceTransportOnLocal(Object endpoint) throws NoSuchFieldException, IllegalAccessException {
        String address = BusTestConsts.SPY_ADDRESS;
        if ((endpoint instanceof NamedBean) && !("busServer".equals(((NamedBean) endpoint).getBeanName()))) {
            address += "/" + ((NamedBean) endpoint).getBeanName();
        }
        Field addressField = getFieldRecursively(endpoint.getClass(), "address");
        addressField.setAccessible(true);
        addressField.set(endpoint, address);

        for (Field addressesField : endpoint.getClass().getDeclaredFields()) {
            if (addressesField.getName().equals("addresses")) {
                addressesField.setAccessible(true);
                addressesField.set(endpoint, Collections.singletonList(address));
            }
        }

        Field transportIdField = getFieldRecursively(endpoint.getClass(), "transportId");
        transportIdField.setAccessible(true);
        transportIdField.set(endpoint, LocalTransportFactory.TRANSPORT_ID);
    }

    private void setDirectDispatch(Object endpoint) throws NoSuchFieldException, IllegalAccessException {
        Field requestContextField = getFieldRecursively(endpoint.getClass(), "requestContext");
        requestContextField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> requestContext = (Map<String, Object>) requestContextField.get(endpoint);
        if (requestContext == null) {
            requestContextField.set(endpoint, requestContext = new HashMap<>());
        }
        requestContext.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
    }

    private void spyBus(Object endpoint) throws IllegalAccessException, NoSuchFieldException {
        final Field busField = getFieldRecursively(endpoint.getClass(), "bus");
        busField.setAccessible(true);
        final Bus endpointBus = (Bus) busField.get(endpoint);
        if (spyBus == DEFAULT_SPY_BUS) {
            spyBus = spy(endpointBus);
        } else {
            checkFeaturesInCommon(spyBus, endpointBus);
        }
        busField.set(endpoint, spyBus);
    }

    private void checkFeaturesInCommon(Bus busOne, Bus busTwo) {
        assertThat(busOne.getFeatures(), equalTo(busTwo.getFeatures()));
    }

    private void mockServer(Object serverEndpoint) throws IllegalAccessException, NoSuchFieldException {
        Field servicesField = getFieldRecursively(serverEndpoint.getClass(), "services");
        servicesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<Object> realServices = (List<Object>) servicesField.get(serverEndpoint);
        for (Object realService : realServices) {
            mockServerServices.add(mock(realService.getClass()));

        }
        servicesField.set(serverEndpoint, mockServerServices);
    }

    private Field getFieldRecursively(Class endpointClass, String fieldName) throws NoSuchFieldException {
        try {
            return endpointClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            final Class superclass = endpointClass.getSuperclass();
            if (superclass != null) {
                return getFieldRecursively(superclass, fieldName);
            } else {
                throw e;
            }
        }
    }
}
