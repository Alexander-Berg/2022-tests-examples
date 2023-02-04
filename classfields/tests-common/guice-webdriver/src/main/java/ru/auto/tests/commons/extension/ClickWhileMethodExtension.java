package ru.auto.tests.commons.extension;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import org.openqa.selenium.WebElement;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.extension.interfaces.ClickWhile;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;

public class ClickWhileMethodExtension implements MethodExtension {

    @Override
    public boolean test(Method method) {
        return method.isAnnotationPresent(ClickWhile.class);
    }

    @Override
    public Object invoke(Object proxy, MethodInfo methodInfo, Configuration configuration) throws Throwable {
        try {
            given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                    .await(format("Кликаем пока элемент %s не станет %s", ((AtlasWebElement) proxy).toString(),
                            methodInfo.getParameter(Matcher.class).get()))
                    .pollInterval(1, SECONDS).atMost(10, SECONDS).ignoreExceptions()
                    .until(() -> {
                        WebElement element = ((AtlasWebElement) proxy);
                        element.click();
                        Matcher matcher = methodInfo.getParameter(Matcher.class).get();
                        return matcher.matches(element);
                    });
        } catch (Exception e) {
        }
        return proxy;
    }
}