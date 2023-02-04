package ru.yandex.realty.element;

import io.qameta.allure.Step;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

public interface CheckButton extends Button {
    @Step("Выбираем кнопку «{name}»")
    default void checkButton(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Кнопка «%s» должна быть выбрана", name)).until(() -> {
            button(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            button(name).waitUntil(hasClass(containsString("_checked")), 1);
            return true;
        });
    }

    @Step("Выбираем кнопку «{name}»")
    default void unCheckButton(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Кнопка «%s» не должна быть выбрана", name)).until(() -> {
            button(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            button(name).waitUntil(hasClass(not(containsString("_checked"))), 1);
            return true;
        });
    }
}
