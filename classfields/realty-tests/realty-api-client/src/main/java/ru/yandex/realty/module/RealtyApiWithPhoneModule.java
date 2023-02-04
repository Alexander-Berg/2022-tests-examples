package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.tests.passport.core.PassportAdaptor;
import ru.auto.tests.passport.module.PassportAccountWithPhoneModule;
import ru.yandex.realty.adaptor.BackRtAdaptor;
import ru.yandex.realty.adaptor.PromocodeAdaptor;
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.anno.Prod;
import ru.yandex.realty.config.RealtyApiConfig;
import ru.yandex.realty.providers.Vos2ApiProdProvider;
import ru.yandex.realty.providers.Vos2ApiProvider;

import static ru.auto.tests.commons.guice.CustomScopes.THREAD;

/**
 * Created by vicdev on 10.07.17.
 */
public class RealtyApiWithPhoneModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new PassportAccountWithPhoneModule());
        install(new Vos2Adaptor());
        install(new BackRtModule());
        install(new BackRtAdaptor());
        install(new SearcherApiModule());
        install(new SearcherAdaptor());
        install(new PassportAdaptor());
        install(new PromoApiModule());
        install(new PromocodeAdaptor());
        install(new BnbSearcherApiModule());

        bind(ApiVos2.class).toProvider(Vos2ApiProvider.class).in(THREAD);
        bind(ApiVos2.class).annotatedWith(Prod.class).toProvider(Vos2ApiProdProvider.class).in(THREAD);
    }

    @Provides
    @Singleton
    private RealtyApiConfig provideConfig() {
        return ConfigFactory.create(RealtyApiConfig.class, System.getProperties(), System.getenv());
    }
}
