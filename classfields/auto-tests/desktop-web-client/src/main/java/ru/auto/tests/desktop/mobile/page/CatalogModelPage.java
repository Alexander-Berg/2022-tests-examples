package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.Offers;
import ru.auto.tests.desktop.mobile.element.catalog.BodyItem;
import ru.auto.tests.desktop.mobile.element.catalog.Gallery;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogModelPage extends CatalogPage, WithCrossLinksBlock {

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'Carousel')]")
    Gallery gallery();

    @Name("Описание модели")
    @FindBy("//div[contains(@class, 'listing-item listing-item_view_promo catalog__item')] | " +
            "//div[contains(@class, 'AmpShowMore')]")
    VertisElement description();

    @Name("Список кузовов")
    @FindBy("//div[contains(@class, 'index-presets__item')] | " +
            "//div[contains(@class, 'GenerationsList')]//div[@class = 'Carousel'] | " +
            "//div[contains(@class, 'GenerationsList')]//div[@class = 'CatalogCarousel']")
    ElementsCollection<BodyItem> bodiesList();

    @Step("Получаем кузов с индексом {i}")
    default BodyItem getBody(int i) {
        return bodiesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Предложения о продаже")
    @FindBy("//div[contains(@class, 'SaleCarousel_mobile')]")
    Offers offers();
}
