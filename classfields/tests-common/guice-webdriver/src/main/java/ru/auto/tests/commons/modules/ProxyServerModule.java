package ru.auto.tests.commons.modules;

import com.google.inject.AbstractModule;
import ru.auto.tests.commons.browsermob.DefaultProxyServerManager;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.auto.tests.commons.guice.CustomScopes;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class ProxyServerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ProxyServerManager.class).to(DefaultProxyServerManager.class).in(CustomScopes.THREAD);
    }
}
