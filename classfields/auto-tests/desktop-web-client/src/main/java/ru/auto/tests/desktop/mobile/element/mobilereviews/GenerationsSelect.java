package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

public interface GenerationsSelect extends VertisElement {

    @Name("Кнопка открытия селекта")
    @FindBy(".//div[contains(@class, 'section__title')]")
    VertisElement selectButton();

    @Name("Поколение «{{ text }}»")
    @FindBy(".//a[.//div[.= '{{ text }}']]")
    VertisElement generationItem(@Param("text") String text);

    @Step("Выбираем поколение {gen}")
    default void selectGeneration(String gen) {
        selectButton().click();
        generationItem(gen).waitUntil(WebElementMatchers.isDisplayed()).click();
    }
}
