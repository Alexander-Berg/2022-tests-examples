package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;

public interface FiltersPopup extends VertisElement, WithRadioButton, WithInput, WithCheckbox, WithButton {

    @Name("Элемент «{{ text }}»")
    @FindBy(".//div[@class = 'ListItem ListItem__root' and .= '{{ text }}'] | " +
            ".//div[@class = 'ListItem ListItem__child' and .= '{{ text }}'] | " +
            ".//div[@class = 'ListItem' and .= '{{ text }}']")
    FiltersPopupItem item(@Param("text") String text);

    @Name("Кнопка применения фильтров")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button")
    VertisElement applyFiltersButton();
}
