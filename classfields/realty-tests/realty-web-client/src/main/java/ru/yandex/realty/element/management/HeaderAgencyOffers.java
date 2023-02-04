package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.SelectButton;

public interface HeaderAgencyOffers extends SelectButton {

    @Name("Чекбокс выделения всех офферов")
    @FindBy(".//label")
    AtlasWebElement selectAllChecbox();

    @Name("Ссылка сортировки «{{ value }}»")
    @FindBy(".//span[contains(., '{{ value }}') and contains(@class, 'Link')]")
    AtlasWebElement sortLink(@Param("value") String value);
}
