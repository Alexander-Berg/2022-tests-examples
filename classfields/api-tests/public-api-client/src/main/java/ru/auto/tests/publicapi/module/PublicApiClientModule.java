package ru.auto.tests.publicapi.module;

import com.google.inject.AbstractModule;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.provider.PublicApiProdProvider;
import ru.auto.tests.publicapi.provider.PublicApiProvider;

import static com.google.inject.Scopes.SINGLETON;

public class PublicApiClientModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiClient.class).toProvider(PublicApiProvider.class).in(SINGLETON);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(PublicApiProdProvider.class).in(SINGLETON);
    }
}
