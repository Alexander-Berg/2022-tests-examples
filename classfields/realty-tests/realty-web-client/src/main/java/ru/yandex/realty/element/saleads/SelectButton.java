package ru.yandex.realty.element.saleads;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.CheckButton;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SelectButton extends AtlasWebElement, Popups, CheckButton {

    @Step("Выбираем «{from}» -> «{to}»")
    default void select(String from, String to) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(format("Кликаем на кнопку %s, ждём отображения попапа", from))
                .pollInterval(2, SECONDS).atMost(10, SECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> {
                    button(from).click();
                    selectPopup().waitUntil(WebElementMatchers.isDisplayed(), 1);
                    return true;
                });
        selectPopup().item(to).click();
        button(to).waitUntil(WebElementMatchers.isDisplayed());
    }

    @Step("Выбираем «{from}» -> «{to}»")
    default void select(String from, String to, String becameButton) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(format("Кликаем на кнопку %s, ждём отображения попапа", from))
                .pollInterval(2, SECONDS).atMost(10, SECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> {
                    button(from).click();
                    selectPopup().waitUntil("", WebElementMatchers.isDisplayed(), 1);
                    return true;
                });
        selectPopup().item(to).waitUntil(WebElementMatchers.isDisplayed()).click();
        button(becameButton).waitUntil(WebElementMatchers.isDisplayed());
    }
}
