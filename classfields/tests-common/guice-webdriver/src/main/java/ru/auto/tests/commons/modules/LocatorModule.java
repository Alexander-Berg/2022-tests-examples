package ru.auto.tests.commons.modules;

import com.google.inject.AbstractModule;
import ru.auto.tests.commons.extension.context.LocatorStorage;
import ru.auto.tests.commons.guice.CustomScopes;

public class LocatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LocatorStorage.class).in(CustomScopes.THREAD);
    }
}
