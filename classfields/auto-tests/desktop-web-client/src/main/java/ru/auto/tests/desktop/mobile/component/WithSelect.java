package ru.auto.tests.desktop.mobile.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.mobile.element.Select;
import ru.auto.tests.desktop.mobile.element.SelectPopup;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface WithSelect {

    @Name("Селект «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Select')][contains(., '{{ value }}')] | " +
            ".//div[contains(@class, 'select ')][contains(., '{{ value }}')] |" +
            ".//div[contains(@class, 'menu-item') and .='{{ value }}']")
    Select select(@Param("value") String value);

    @Name("Поп-ап")
    @FindBy("(//div[contains(@class,'Popup_visible')])[last()] | " +
            "(//div[contains(@class,'popup_visible')])[last()]")
    SelectPopup selectPopup();

    @Step("В селекте «{selectName}» выбираем «{option}»")
    default void selectItem(String selectName, String option) {
        select(selectName).selectButton().should(isDisplayed()).should(isEnabled()).hover().click();
        selectPopup().waitUntil(isDisplayed());
        selectPopup().item(option).click();
    }

}
