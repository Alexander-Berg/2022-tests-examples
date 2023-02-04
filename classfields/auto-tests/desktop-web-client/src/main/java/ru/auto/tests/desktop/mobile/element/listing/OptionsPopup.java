package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface OptionsPopup extends VertisElement, WithRadioButton, WithInput, WithCheckbox, WithButton {

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[@class = 'ListItem' and .= '{{ text }}']")
    Option option(@Param("text") String text);

    @Name("Кнопка применения фильтров")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button")
    VertisElement applyFiltersButton();
}
