package ru.auto.tests.canonical;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;

public class CanonicalClientModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    private CanonicalConfig provideCanonicalConfig() {
        return ConfigFactory.create(CanonicalConfig.class, System.getProperties(), System.getenv());
    }
}
