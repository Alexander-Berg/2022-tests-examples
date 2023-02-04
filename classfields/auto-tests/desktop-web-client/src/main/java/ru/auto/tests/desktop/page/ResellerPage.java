package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithSalesList;
import ru.auto.tests.desktop.component.WithSelect;

public interface ResellerPage extends BasePage, WithSelect, WithSalesList, WithPager {

    String IN_STOCK = "В наличии";
    String SOLD = "Продано";
    String SORT = "Сортировка";

    @Name("Кол-во активных офферов")
    @FindBy("//li[contains(@class, '_headListItem')][1]")
    VertisElement activeOffers();

    @Name("Кол-во неактивных офферов")
    @FindBy("//li[contains(@class, '_headListItem')][2]")
    VertisElement inactiveOffers();

    @Name("Лет на Авто.ру")
    @FindBy("//li[contains(@class, '_headListItem')][3]")
    VertisElement years();

    @Name("Статус офферов «{{ status }}»")
    @FindBy("//span[contains(@class, 'ResellerPublicFilters__status')]//button[.='{{ status }}']")
    VertisElement status(@Param("status") String status);

}
