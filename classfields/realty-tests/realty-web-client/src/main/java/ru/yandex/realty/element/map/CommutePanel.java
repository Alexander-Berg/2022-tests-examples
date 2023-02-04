package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;

public interface CommutePanel extends Link {

    @Name("Кнопка «Общественный траспорт»")
    @FindBy(".//div[contains(@class,'MapCommuteTransportPanel__item')][.//i[contains(@class,'commute-public-transport')]]")
    AtlasWebElement publicTransportButton();

    @Name("Кнопка «Автомобиль»")
    @FindBy(".//div[contains(@class,'MapCommuteTransportPanel__item')][.//i[contains(@class,'commute-auto')]]")
    AtlasWebElement autoTransportButton();

    @Name("Кнопка «Пешком»")
    @FindBy(".//div[contains(@class,'MapCommuteTransportPanel__item')][.//i[contains(@class,'commute-by-foot')]]")
    AtlasWebElement byFootButton();

    @Name("Кнопка селектора времени")
    @FindBy(".//div[@class='MapSelect__button-content']")
    AtlasWebElement timeSelectButton();

    @Name("Элемент саджеста времени «{{ value }}»")
    @FindBy(".//div[contains(@class,'MapSelect__content_expanded')]//div[contains(@class,'MapSelectItem')]" +
            "[contains(.,'{{ value }}')]")
    AtlasWebElement timeSuggestItem(@Param("value") String value);
}
