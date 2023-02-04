package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Popups {

    @Name("Попап селекта")
    @FindBy("//div[contains(@class, 'Popup_js_inited')][contains(@class, 'Popup_visible')]")
    SelectPopup filterPopup();

    default SelectPopup selectPopup() {
        filterPopup().should(isDisplayed());
        return filterPopup();
    }
}
