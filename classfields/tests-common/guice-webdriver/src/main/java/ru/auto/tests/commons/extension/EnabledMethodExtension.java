package ru.auto.tests.commons.extension;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Method;
import java.util.Optional;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class EnabledMethodExtension implements MethodExtension {

    @Override
    public boolean test(Method method) {
        return method.isAnnotationPresent(EnabledMethod.class);
    }

    @Override
    public Object invoke(Object proxy,
                         MethodInfo methodInfo,
                         Configuration configuration) throws Throwable {
        try {
            Optional<WebDriver> driver = configuration.getContext(WebDriver.class);
            if (driver.isPresent()) {
                WebElement element = ((AtlasWebElement) proxy).waitUntil(isDisplayed());
                if (!element.isEnabled()) {
                    throw new AssertionError("element not enabled");
                }
            }
        } catch (Exception e) {
        }
        return proxy;
    }

}