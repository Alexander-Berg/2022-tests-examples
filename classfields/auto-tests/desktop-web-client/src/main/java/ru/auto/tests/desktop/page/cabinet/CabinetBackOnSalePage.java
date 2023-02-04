package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.cabinet.WithCalendar;
import ru.auto.tests.desktop.element.cabinet.WithMenuPopup;
import ru.auto.tests.desktop.element.cabinet.backonsale.Filters;
import ru.auto.tests.desktop.element.cabinet.backonsale.Listing;
import ru.auto.tests.desktop.element.cabinet.backonsale.SortsBlock;
import ru.auto.tests.desktop.element.header.SubHeader;
import ru.auto.tests.desktop.element.main.GeoSelectPopup;

public interface CabinetBackOnSalePage extends BasePage, WithMenuPopup, WithCalendar, WithPager, WithActivePopup,
        SubHeader {

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'BackOnSale']")
    VertisElement content();

    @Name("Изображение (логотип) страницы")
    @FindBy("//img[@class = 'BackOnSale__image']")
    VertisElement image();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//button[.= '{{ text }}']")
    VertisElement button(@Param("text") String Text);

    @Name("Блок фильтров")
    @FindBy("//div[@class = 'BackOnSaleFilters BackOnSale__filters']")
    Filters filters();

    @Name("Блок сортировки офферов")
    @FindBy("//div[@class = 'BackOnSaleSorts BackOnSale__sorts']")
    SortsBlock sortsBlock();

    @Name("Листинг объявлений")
    @FindBy("//div[@class = 'BackOnSale__listing']")
    Listing listing();

}
