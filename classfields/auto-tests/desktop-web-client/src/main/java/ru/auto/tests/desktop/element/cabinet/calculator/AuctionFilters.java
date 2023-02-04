package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AuctionFilters extends VertisElement {

    @Name("Фильтр по марке «{{ mark }}»")
    @FindBy("./div[contains(@class, 'Tags')]/span[contains(@class, 'Tags__tag') and .='{{ mark }}']")
    VertisElement filter(@Param("mark") String mark);

    @Name("Чекбокс «Только участники аукциона»")
    @FindBy("./label[contains(@class, 'Auction__checkbox') and .//span[.='Только участники аукциона • 2']]" +
            "//input[@type='checkbox']")
    VertisElement auctionCheckbox();

}
