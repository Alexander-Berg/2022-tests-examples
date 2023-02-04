package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 07.12.18
 */
public interface Services extends VertisElement {

    String PREMIUM_SERVICE = "Премиум";
    String RAISING_IN_SEARCH_SERVICE = "Поднятие в поиске";
    String SPECIAL_OFFER_SERVICE = "Специальное предложение";
    String STICKERS_SERVICE = "Стикеры";

    @Name("Услуга «{{ name }}»")
    @FindBy(".//tr[@class='CalculatorServicesTable__row'][contains(., '{{ name }}')]")
    ServicesBlock service(@Param("name") String name);
}
