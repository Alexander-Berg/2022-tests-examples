package ru.auto.tests.commons.modules;

import com.google.inject.AbstractModule;
import ru.auto.tests.commons.guice.CustomScopes;
import ru.auto.tests.commons.utils.DefaultMarkerManager;
import ru.auto.tests.commons.utils.MarkerManager;

/**
 * Created by vicdev on 14.08.17.
 */
public class MarkerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MarkerManager.class).to(DefaultMarkerManager.class).in(CustomScopes.THREAD);
    }
}
