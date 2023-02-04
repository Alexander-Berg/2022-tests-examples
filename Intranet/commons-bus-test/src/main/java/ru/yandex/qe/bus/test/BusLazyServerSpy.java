package ru.yandex.qe.bus.test;

import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.yandex.qe.bus.factories.BusServerBuilderBean;

/**
 * User: terry
 * Date: 01.09.13
 * Time: 18:36
 */
public class BusLazyServerSpy implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private Server spyServer;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public <T> T mockViaOnDemandSpyServer(Bus spyBus, final Class<T> serviceClass) {
        checkNoSpyServer();
        try {
            final T mockServerService = Mockito.mock(serviceClass);
            final BusServerBuilderBean busServerFactoryBean = new BusServerBuilderBean();
            busServerFactoryBean.setApplicationContext(applicationContext);
            busServerFactoryBean.setTransportId(LocalTransportFactory.TRANSPORT_ID);
            busServerFactoryBean.setAddress(BusTestConsts.SPY_ADDRESS);
            busServerFactoryBean.setBus(spyBus);

            @SuppressWarnings("unchecked")
            final Map<Object, Object> defaultExtensionMappings =
                    (Map<Object, Object>) applicationContext.getBean("busExtensionMappings");
            busServerFactoryBean.setExtensionMappings(defaultExtensionMappings);

            final List<?> defaultBusProviders = (List<?>) applicationContext.getBean("busProviders");
            busServerFactoryBean.setProviders(defaultBusProviders);

            busServerFactoryBean.setResource(new SingletonResourceProvider(mockServerService) {
                @Override
                public Class<?> getResourceClass() {
                    return serviceClass;
                }
            });

            spyServer = busServerFactoryBean.getObject();
            return mockServerService;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkNoSpyServer() {
        if (spyServer != null) {
            throw new RuntimeException("only one mock access per test supported");
        }
    }

    public void stop() {
        if (spyServer != null) {
            spyServer.stop();
            spyServer = null;
        }
    }

}
