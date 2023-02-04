package ru.auto.tests.commons.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.commons.guice.CustomScopes;
import ru.auto.tests.commons.webdriver.DefaultWebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverConfig;
import ru.auto.tests.commons.webdriver.WebDriverManager;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class WebDriverModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(WebDriverManager.class).to(DefaultWebDriverManager.class).in(CustomScopes.THREAD);
    }

    @Provides
    public WebDriverConfig provideConfig() {
        return ConfigFactory.create(WebDriverConfig.class, System.getProperties(), System.getenv());
    }

}
