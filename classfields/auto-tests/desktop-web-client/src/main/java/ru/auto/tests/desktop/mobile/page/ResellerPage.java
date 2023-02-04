package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithPager;
import ru.auto.tests.desktop.mobile.component.WithSalesList;
import ru.auto.tests.desktop.mobile.component.WithSelect;
import ru.auto.tests.desktop.mobile.component.WithSortBar;

public interface ResellerPage extends BasePage, WithSalesList, WithPager, WithSelect, WithSortBar {

    String IN_STOCK = "В наличии";
    String SOLD = "Продано";

    @Name("Лет на Авто.ру")
    @FindBy("//li[contains(@class, '_headListItem')][1]")
    VertisElement years();

    @Name("Кол-во офферов")
    @FindBy("//li[contains(@class, '_headListItem')][2]")
    VertisElement offersCount();

    @Name("Статус офферов «{{ status }}»")
    @FindBy("//span[contains(@class, 'ResellerPublicFiltersMobile__status')]//button[.='{{ status }}']")
    VertisElement status(@Param("status") String status);

    @Name("Сортировка")
    @FindBy("//div[contains(@class, 'Tabs__sort')]")
    VertisElement sort();

}
