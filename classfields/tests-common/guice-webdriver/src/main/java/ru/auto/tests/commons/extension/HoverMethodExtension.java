package ru.auto.tests.commons.extension;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.context.WebDriverContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.extension.interfaces.HoverMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class HoverMethodExtension implements MethodExtension {

    @Override
    public boolean test(Method method) {
        return method.isAnnotationPresent(HoverMethod.class);
    }

    @Override
    public Object invoke(Object proxy,
                         MethodInfo methodInfo,
                         Configuration configuration) throws Throwable {
        try {
            Optional<WebDriverContext> driver = configuration.getContext(WebDriverContext.class);
            if (driver.isPresent()) {
                WebElement element = ((AtlasWebElement) proxy).waitUntil(isDisplayed());
                new Actions(driver.get().getValue()).moveToElement(element).build().perform();
            }
        } catch (Exception e) {
        }
        return proxy;
    }

}