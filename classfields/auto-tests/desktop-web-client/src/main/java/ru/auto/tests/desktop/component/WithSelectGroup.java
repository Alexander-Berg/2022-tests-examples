package ru.auto.tests.desktop.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.element.listing.SelectGroup;
import ru.auto.tests.desktop.element.listing.SelectPopup;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

public interface WithSelectGroup {

    @Name("Группа селектов «{{ value }}»")
    @FindBy(".//span[contains(@class, 'ControlGroup')][contains(., '{{ value }}')]")
    SelectGroup selectGroup(@Param("value") String value);

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    SelectPopup selectPopup();

    @Step("В группе селектов «{groupName}» выбираем «{option}»")
    default void selectGroupItem(String groupName, String selectName, String option) {
        selectGroup(groupName).selectButton(selectName).should(WebElementMatchers.isDisplayed()).hover().click();
        selectPopup().waitUntil(WebElementMatchers.isDisplayed());
        selectPopup().item(option).click();
    }
}
