package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Presets extends VertisElement {

    @Name("Пресет «{{ text }}»")
    @FindBy(".//li[contains(@class, 'tabs__item') and .= '{{ text }}']")
    VertisElement preset(@Param("text") String text);

    @Name("Кнопка «Следующие»")
    @FindBy(".//div[contains(@class, 'index-presets-content__more')]")
    VertisElement showMore();

    @Name("Кнопка «Показать все»")
    @FindBy(".//a[contains(@class, 'index-presets-content__all')]")
    VertisElement showAll();

    @Name("Список элементов пресета")
    @FindBy(".//div[@class = 'index-presets__item'] | " +
            ".//div[@class = 'Carousel'] | " +
            ".//div[@class = 'CatalogCarousel']")
    ElementsCollection<VertisElement> presetItemsList();

    @Step("Получаем элемент пресета с индексом {i}")
    default VertisElement getPresetItem(int i) {
        return presetItemsList().should(hasSize(greaterThan(i))).get(i);
    }
}