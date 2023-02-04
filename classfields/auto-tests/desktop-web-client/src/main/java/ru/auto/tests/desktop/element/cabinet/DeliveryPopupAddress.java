package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface DeliveryPopupAddress extends VertisElement, WithInput, WithGeoSuggest {

    @Name("Стрелка")
    @FindBy(".//div[contains(@class, 'DeliverySettingsRegion__arrow')]")
    VertisElement arrow();

    @Name("Кнопка удаления")
    @FindBy(".//div[contains(@class, 'DeliverySettingsRegion__removeButton')]")
    VertisElement deleteButton();

    @Name("Кнопка «Отменить удаление»")
    @FindBy(".//li[contains(@class, 'DeliverySettingsRegion_deleted')]")
    VertisElement undoDeleteButton();

    @Name("Услуги")
    @FindBy(".//ul[contains(@class, 'DeliverySettingsRegion__services_expanded')]")
    VertisElement services();
}