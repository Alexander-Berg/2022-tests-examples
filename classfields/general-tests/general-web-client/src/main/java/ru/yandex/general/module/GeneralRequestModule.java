package ru.yandex.general.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.aeonbits.owner.ConfigFactory;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;

public class GeneralRequestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MockRuleWithoutWebdriver.class);
    }

    @Provides
    @Singleton
    public GeneralWebConfig provideGeneralWebConfig() {
        return ConfigFactory.create(GeneralWebConfig.class, System.getProperties(), System.getenv());
    }
}
