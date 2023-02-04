package ru.auto.tests.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.api.SearcherClient;
import ru.auto.tests.api.SearcherConfig;
import ru.auto.tests.api.provider.SearcherClientProvider;
import ru.auto.tests.commons.guice.CustomScopes;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class SearcherModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SearcherClient.class).toProvider(SearcherClientProvider.class).in(CustomScopes.THREAD);
    }

    @Provides
    public SearcherConfig provideSearcherConfig() {
        return ConfigFactory.create(SearcherConfig.class, System.getProperties(), System.getenv());
    }

}
