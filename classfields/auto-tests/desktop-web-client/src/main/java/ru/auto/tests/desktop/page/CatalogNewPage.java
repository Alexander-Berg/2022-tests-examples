package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithOffers;
import ru.auto.tests.desktop.component.WithVideos;
import ru.auto.tests.desktop.element.catalogNew.card.Gallery;
import ru.auto.tests.desktop.element.catalogNew.card.SpecificationContent;
import ru.auto.tests.desktop.element.catalogNew.card.SpecificationLinks;
import ru.auto.tests.desktop.element.listing.Filter;

public interface CatalogNewPage extends BasePage, WithVideos, WithOffers {

    String GENERATION_TAB = "Поколения";
    String SPECIFICATION_TAB = "Характеристики";

    String MARK = "Audi";
    String MODEL = "A5";

    @Name("Галерея")
    @FindBy("//div[@class = 'CatalogGallery']")
    Gallery gallery();

    @Name("Таб «{{ text }}»")
    @FindBy("//a[contains(@class, 'SpecificationsNavigation__link') and .='{{ text }}'] | " +
    "//span[contains(@class, 'SpecificationsNavigation__link') and .='{{ text }}']")
    VertisElement navigationTab(@Param("text") String text);

    @Name("Фильтр")
    @FindBy("//div[contains(@class,'CatalogFilterForm')]")
    Filter filter();

    @Name("Блок ссылок на переключение характеристик")
    @FindBy("//div[@class = 'SpecificationLinks']")
    SpecificationLinks specificationLinksBlock();

    @Name("Контент характеристик")
    @FindBy("//div[@class = 'SpecificationContent']")
    SpecificationContent specificationContentBlock();
}
