package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface GeoPopup extends VertisElement, WithInput, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'FiltersPopup__title')]")
    VertisElement title();

    @Name("Кнопка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'FiltersPopup__close')]")
    VertisElement closeButton();

    @Name("Кнопка поиска региона")
    @FindBy(".//div[contains(@class, 'GeoSelectPopup__search-input')]")
    VertisElement searchRegionButton();

    @Name("Группа регионов «{{ text }}»")
    @FindBy(".//div[contains(@class, 'CheckboxTreeGroup') and .//div[.= '{{ text }}']]")
    GeoPopupRegionGroup regionGroup(@Param("text") String text);

    @Name("Регион «{{ text }}»")
    @FindBy(".//div[contains(@class, 'CheckboxTreeGroup__checkbox') and .= '{{ text }}']")
    VertisElement region(@Param("text") String text);

    @Name("Кнопка «Сбросить»")
    @FindBy(".//div[contains(@class, 'FiltersPopup__reset')]")
    VertisElement resetButton();

    @Name("Кнопка «Готово»")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button")
    VertisElement readyButton();
}
