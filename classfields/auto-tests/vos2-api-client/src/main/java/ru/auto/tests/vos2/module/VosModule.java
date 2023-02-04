package ru.auto.tests.vos2.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.commons.guice.CustomScopes;
import ru.auto.tests.vos2.VosClientRetrofit;
import ru.auto.tests.vos2.VosConfig;
import ru.auto.tests.vos2.provider.VosClientProvider;

public class VosModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(VosClientRetrofit.class).toProvider(VosClientProvider.class).in(CustomScopes.THREAD);
    }

    @Provides
    public VosConfig provideVosConfig() {
        return ConfigFactory.create(VosConfig.class, System.getProperties(), System.getenv());
    }

}
