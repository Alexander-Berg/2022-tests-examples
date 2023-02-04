package ru.auto.tests.desktop.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.element.Select;
import ru.auto.tests.desktop.element.listing.SelectPopup;

import java.util.Arrays;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

public interface WithSelect {

    @Name("Селект «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Select')][contains(., '{{ value }}')] | " +
            ".//div[contains(@class, 'select ')][contains(., '{{ value }}')] |" +
            ".//div[contains(@class, 'menu-item') and .='{{ value }}'] |" +
            ".//div[contains(@class, 'MenuItem ') and .='{{ value }}']")
    Select select(@Param("value") String value);

    @Name("Поп-ап")
    @FindBy("(//div[contains(@class,'Popup_visible')])[last()] | " +
            "(//div[contains(@class,'popup_visible')])[last()]")
    SelectPopup selectPopup();

    @Step("В селекте «{selectName}» выбираем «{option}»")
    default void selectItem(String selectName, String... options) {
        select(selectName).selectButton().should(isDisplayed()).should(isEnabled()).hover().click();
        selectPopup().waitUntil(isDisplayed());
        Arrays.stream(options).forEach(option -> selectPopup().item(option).click());
    }
}
