package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.cardpage.Stats;
import ru.auto.tests.desktop.mobile.element.catalog.Filter;
import ru.auto.tests.desktop.mobile.element.catalog.ModelsItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogPage extends BasePage, WithCrossLinksBlock {

    @Name("Фильтр")
    @FindBy("//div[@class = 'Filters'] | " +
            "//div[@class = 'CatalogFilters']")
    Filter filter();

    @Name("Блок объявлений")
    @FindBy("//div[contains(@class, 'banner_type_catalog')] | " +
            "//div[@class = 'Counter']")
    VertisElement sales();

    @Name("Блок «Как дешевеет это авто с возрастом»")
    @FindBy("//div[contains(@class, 'PriceStatsInfo')]")
    Stats stats();

    @Name("Список моделей")
    @FindBy("//div[contains(@class, 'index-presets__item')] | " +
            "//div[@class = 'Carousel']")
    ElementsCollection<ModelsItem> modelsList();

    @Step("Получаем модель с индексом {i}")
    default ModelsItem getModel(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Рекламный блок с2")
    @FindBy("//div[contains(@class, 'catalog__features')]/div/ancestor::div")
    VertisElement c2advert();

    @Name("Кнопка «Предыдущие»")
    @FindBy("//div[@class='index-presets-content__more']")
    VertisElement prevButton();

    @Name("Электро баннер")
    @FindBy("//div[contains(@class, '_electroBanner')]")
    VertisElement electroBanner();

}
