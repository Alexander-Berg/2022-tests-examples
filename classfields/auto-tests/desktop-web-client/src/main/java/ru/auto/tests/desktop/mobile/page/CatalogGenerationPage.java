package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.Offers;
import ru.auto.tests.desktop.mobile.element.catalog.BodyItem;
import ru.auto.tests.desktop.mobile.element.catalog.Gallery;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogGenerationPage extends CatalogPage, WithCrossLinksBlock {

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'Carousel')]")
    Gallery gallery();

    @Name("Список кузовов")
    @FindBy("//div[contains(@class, 'index-presets__item')] | " +
            "//div[contains(@class, 'GenerationsList')]//div[@class = 'Carousel']")
    ElementsCollection<BodyItem> bodiesList();

    @Step("Получаем кузов с индексом {i}")
    default BodyItem getBody(int i) {
        return bodiesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Предложения о продаже")
    @FindBy("//div[contains(@class, 'SaleCarousel_mobile')]")
    Offers offers();
}
