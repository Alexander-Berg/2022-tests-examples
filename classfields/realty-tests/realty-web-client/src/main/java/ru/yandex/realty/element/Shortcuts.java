package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Shortcuts {

    String PROGRESS = "Ход строительства";
    String PROFITABILITY = "Окупаемость";
    String LOCATION = "Расположение";
    String PANORAMA = "Панорама";
    String NEAR_OFFERS = "Предложения рядом";
    String INFRASTRUCTURE = "Инфраструктура";
    String TRANSPORT = "Транспорт";
    String TRANSPORT_ACCESSIBILITY = "Транспорт";
    String SELL_COST = "Стоимость жилья";
    String YA_DRIVE = "Яндекс.Драйв";
    String SELL_PRICE = "Цена продажи";
    String RENT_COST = "Стоимость аренды";
    String RENT_PRICE = "Цена аренды";
    String GENPLAN = "Генплан";

    @Name("Шорткаты")
    @FindBy("//div[contains(@class,'CardShortcuts__wrapper')]")
    AtlasWebElement shortcutsContainer();

    @Name("Шорткат «{{ value }}»")
    @FindBy("//div[contains(@class,'CardShortcutsItem_type_') and contains(.,'{{ value }}')]")
    AtlasWebElement shortcut(@Param("value") String value);

    @Name("Кнопка прокрутки шоркатов")
    @FindBy("//button[contains(@class,'CardShortcuts__forward_new')]")
    AtlasWebElement swipeShortcutsForward();

    @Name("Кнопка прокрутки шоркатов")
    @FindBy("//div[@class='CardShortcuts__forward']//i")
    AtlasWebElement swipeShortcutsForwardOld();

    @Name("Шорткат карты «{{ value }}»")
    @FindBy("//div[contains(@class,'MapLayersPanel')]//button[contains(.,'{{ value }}')]")
    AtlasWebElement mapShortcut(@Param("value") String value);
}
