package ru.yandex.qe.bus.test;

import java.util.List;

import javax.inject.Inject;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:spring/bus-spy.xml"})
public abstract class AbstractBusJUnit5SpringContextTests implements ApplicationContextAware {

    @Inject
    private BusEndpointsSpy busEndpointsSpy;

    @Inject
    private BusLazyServerSpy busLazyServerSpy;

    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @AfterEach
    protected void resetEndpointMocks() {
        busEndpointsSpy.resetMocks();
        busLazyServerSpy.stop();
    }

    protected <T> T getMockServerService(Class<T> serviceClass) {
        if (isBusServicePresentInContext()) {
            return busEndpointsSpy.getMockServerService(serviceClass);
        }
        return busLazyServerSpy.mockViaOnDemandSpyServer(busEndpointsSpy.getSpyBus(), serviceClass);
    }

    private boolean isBusServicePresentInContext() {
        try {
            applicationContext.getBean(Server.class);
            return true;
        } catch (NoSuchBeanDefinitionException ex) {
            return false;
        }
    }

    protected WebClient createWebClient(Object clientService) {
        return WebClient.fromClient((Client) clientService);
    }

    protected WebClient createLocalClient() {
        final List<?> defaultBusProviders = (List<?>) applicationContext.getBean("busProviders");
        final WebClient webClient = WebClient.create(BusTestConsts.SPY_ADDRESS, defaultBusProviders);
        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.setBus(busEndpointsSpy.getSpyBus());
        config.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return webClient;
    }

    protected WebClient createLocalClient(String busName) {
        final List<?> defaultBusProviders = (List<?>) applicationContext.getBean("busProviders");
        final WebClient webClient = WebClient.create(BusTestConsts.SPY_ADDRESS + "/" + busName, defaultBusProviders);
        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.setBus(busEndpointsSpy.getSpyBus());
        config.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return webClient;
    }
}
