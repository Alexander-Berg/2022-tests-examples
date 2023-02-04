package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.catalog.MarkDescription;
import ru.auto.tests.desktop.mobile.element.catalog.ModelsItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogMarkPage extends CatalogPage, WithCrossLinksBlock {

    @Name("Кнопка «Смотреть все»")
    @FindBy("//div[contains(@class, 'banner_type_catalog')] | " +
            "//div[@class = 'Counter']")
    VertisElement showAllBlock();

    @Name("Описание марки")
    @FindBy("//div[contains(@class, 'listing-item listing-item_view_promo catalog__item')] | " +
            "//div[contains(@class, 'AmpShowMore')]")
    MarkDescription description();

    @Name("Список моделей")
    @FindBy("//div[contains(@class, 'index-presets__item')] | " +
            "//div[@class = 'Carousel'] | " +
            "//div[@class = 'CatalogCarousel']")
    ElementsCollection<ModelsItem> modelsList();

    @Step("Получаем модель с индексом {i}")
    default ModelsItem getModel(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }
}