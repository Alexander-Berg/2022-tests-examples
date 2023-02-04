package ru.auto.tests.redirect;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;

public class RedirectClientModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    private RedirectConfig provideRedirectConfig() {
        return ConfigFactory.create(RedirectConfig.class, System.getProperties(), System.getenv());
    }
}
