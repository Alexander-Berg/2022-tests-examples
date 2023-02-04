package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithPager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogMarkPage extends BasePage, WithPager {

    @Name("Переключалка Все/Популярные")
    @FindBy("//div[contains(@class, 'catalog__controls')]//span[text() = '{{ text }}']")
    VertisElement popularAllSwitcher(@Param("text") String text);

    @Name("Популярные модели")
    @FindBy("//h2[text() = 'Популярные']//..")
    VertisElement popularModels();

    @Name("Все модели")
    @FindBy("//h2[text() = 'Все модели']//..")
    VertisElement allModels();

    @Name("Список популярных моделей в табличном виде")
    @FindBy("//h2[text() = 'Популярные']/..//div[contains(@class, 'mosaic mosaic_size_s')]")
    ElementsCollection<VertisElement> popularModelsListInTableViewType();

    @Name("Список всех моделей в табличном виде")
    @FindBy("//h2[contains(., 'Все модели')]/..//div[contains(@class, ' mosaic_size_')]")
    ElementsCollection<VertisElement> allModelsListInTableViewType();

    @Name("Модели в списочном виде")
    @FindBy("//div[contains(@class, 'catalog__index-list')]")
    VertisElement modelsInListViewType();

    @Step("Получаем модель с индексом {i}")
    default VertisElement getModel(int i) {
        return popularModelsListInTableViewType().should(hasSize(greaterThan(i))).get(i);
    }
}