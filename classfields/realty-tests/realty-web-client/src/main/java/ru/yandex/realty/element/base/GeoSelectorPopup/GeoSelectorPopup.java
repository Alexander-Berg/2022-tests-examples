package ru.yandex.realty.element.base.GeoSelectorPopup;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.Popups;
import ru.yandex.realty.element.saleads.SelectionBlock;

/**
 * Created by vicdev on 25.05.17.
 */
public interface GeoSelectorPopup extends Link, SelectionBlock, Button, Popups {

    String FROM_CITY = "Удаленность от";
    String METRO = "Метро";

    @Name("Контент геоселектора")
    @FindBy(".//div[contains(@class,'Modal__tab_activeTab')]")
    AtlasWebElement content();

    @Name("Контент карты")
    @FindBy(".//div[contains(@class, 'Map_ready')]")
    GeoSelectorContent mapContent();

    @Name("Поле ввода времени до метро")
    @FindBy(".//div[contains(@class, 'MetroTimeSelector')]//button")
    AtlasWebElement timeToMetroInput();

    @Name("Кнопка для выбора типа транспорта до метро")
    @FindBy(".//div[contains(@class, 'MetroTransportSelector')]//button")
    AtlasWebElement metroTransport();

    @Name("Кнопка для выбора линии метро")
    @FindBy(".//div[contains(@class, 'MetroLinesSelector')]//button")
    AtlasWebElement metroLineButton();

    @Name("Саджест метро")
    @FindBy(".//div[contains(@class, 'MetroStationsSuggest')]")
    MetroSuggest metroSuggest();

    @Name("Таб «{{ value }}»")
    @FindBy(".//span[contains(@class, 'Tab') and contains(., '{{ value }}')]")
    AtlasWebElement tab(@Param("value") String value);

    @Name("Кнопка «Показать»")
    @FindBy("//button[contains(@class,'CounterButton')]")
    AtlasWebElement submitButton();
}
