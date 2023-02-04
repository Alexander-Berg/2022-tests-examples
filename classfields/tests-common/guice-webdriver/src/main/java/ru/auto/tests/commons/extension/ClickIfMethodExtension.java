package ru.auto.tests.commons.extension;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import org.openqa.selenium.WebElement;
import ru.auto.tests.commons.extension.interfaces.ClickIfMethod;

import java.lang.reflect.Method;

public class ClickIfMethodExtension implements MethodExtension {

    @Override
    public boolean test(Method method) {
        return method.isAnnotationPresent(ClickIfMethod.class);
    }

    @Override
    public Object invoke(Object proxy, MethodInfo methodInfo, Configuration configuration) throws Throwable {
        try {
            WebElement element = ((AtlasWebElement) proxy);
            Matcher matcher = methodInfo.getParameter(Matcher.class).get();
            if (matcher.matches(element)) {
                element.click();
            }
        } catch (Exception e) {
        }
        return proxy;
    }

}