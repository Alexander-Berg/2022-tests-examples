package ru.yandex.realty.element.saleads;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;

import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SelectionBlock extends Button {

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//label[contains(@class, 'Checkbox_js_inited')][contains(., '{{ value }}')]")
    AtlasWebElement checkBox(@Param("value") String value);

    @Name("Список чекбоксов")
    @FindBy(".//label[contains(@class, 'Checkbox_js_inited')]")
    ElementsCollection<RealtyElement> checkBoxList();

    @Step("Выбираем радиокнопку {name}")
    default void selectButton(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Кнопка «%s» должна быть выбрана", name)).until(() -> {
                    button(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            button(name).waitUntil(hasClass(containsString("_checked")), 1);
                    return true;
        });
    }

    @Step("Выбираем радиокнопку {name}")
    default void deSelectButton(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Кнопка «%s» НЕ должна быть выбрана", name)).until(() -> {
            button(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            button(name).waitUntil(hasClass(not(containsString("_checked"))), 1);
            return true;
        });
    }

    @Step("Выбираем чекбокс {name}")
    default void selectCheckBox(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Чекбокс «%s» должен быть выбран", name)).until(() -> {
                    checkBox(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            checkBox(name).waitUntil(hasClass(containsString("_checked")), 1);
                    return true;
        });
    }

    @Step("Снимаем выбор с чекбокса {name}")
    default void deselectCheckBox(String name) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .atMost(20, SECONDS).alias(format("Чекбокс «%s» должен быть пустым", name)).until(() -> {
                    checkBox(name).waitUntil(WebElementMatchers.isDisplayed()).click();
            checkBox(name).waitUntil("", not(hasClass(containsString("_checked"))), 1);
                    return true;
        });
    }

    default void selected(String name) {
        checkBox(name).should(hasClass(containsString("checked")));
    }

    default void notSelected(String name) {
        checkBox(name).should(not(hasClass(containsString("checked"))));
    }

    default void selectAll(String... names) {
        Stream.of(names).forEach(this::selectCheckBox);
    }

}
