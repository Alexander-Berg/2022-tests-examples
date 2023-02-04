package ru.auto.tests.desktop.mobile.element.group;

import io.qameta.allure.Description;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface FiltersPopup extends VertisElement, WithRadioButton, WithInput, WithCheckbox, WithButton {

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[@class = 'ListItem' and .= '{{ text }}']")
    FiltersPopupItem item(@Param("text") String text);

    @Name("Опция, в названии которой есть «{{ text }}»")
    @FindBy(".//div[@class = 'ListItem' and contains(., '{{ text }}')]")
    FiltersPopupItem itemContains(@Param("text") String text);

    @Description("Комплектация «{{ text }}»")
    @FindBy(".//div[contains(@class, 'CardGroupFilterComplectationItem') and contains(., '{{ text }}')]")
    VertisElement complectation(@Param("text") String Text);

    @Description("Выбранная комплектация")
    @FindBy(".//div[contains(@class, 'CardGroupFilterComplectationItem_selected')]/div[contains(@class, 'name')]")
    VertisElement selectedComplectation();

    @Name("Кнопка применения фильтров")
    @FindBy(".//div[contains(@class, 'Modal__content_footer')]//button")
    VertisElement applyFiltersButton();
}
