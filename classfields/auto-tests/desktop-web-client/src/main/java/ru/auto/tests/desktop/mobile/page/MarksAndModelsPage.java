package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MarksAndModelsPage extends BasePage {

    @Name("Популярные марки")
    @FindBy("//div[contains(@class, 'reference__list')]")
    VertisElement popularMarks();

    @Name("Все марки")
    @FindBy("//ul[contains(@class, 'reference__list')] | " +
            "//div[@class = 'BreadcrumbsFilterableList__list']")
    VertisElement allMarks();

    @Name("Марка")
    @FindBy("//a[contains(@class, 'reference__item-url')]//div[.= '{{ text }}'] | " +
            "//span[contains(@class, 'Link BreadcrumbsFilterableListItem__link')]//div[.= '{{ text }}']")
    VertisElement mark(@Param("text") String mark);

    @Name("Поле ввода названия марки/модели")
    @FindBy("//div[@class = 'reference__search']//input | " +
            "//div[contains(@class, 'TextInput__input')]//input")
    VertisElement searchInput();

    @Name("Популярные модели")
    @FindBy("//div[contains(@class, 'reference__list')]")
    VertisElement popularModels();

    @Name("Все модели")
    @FindBy("//ul[contains(@class, 'reference__list')]")
    VertisElement allModels();

    @Name("Модель '{{ text }}'")
    @FindBy("//a[contains(@class, 'reference__item-url')]//div[text() = '{{ text }}'] | " +
            "//div[contains(@class, 'BreadcrumbsFilterableListItem__name') and .= '{{ text }}']")
    VertisElement model(@Param("text") String model);

    @Name("Список марок/моделей")
    @FindBy("//a[contains(@class, 'reference__item-url')] | " +
            "//div[@class = 'BreadcrumbsFilterableListItem']")
    ElementsCollection<VertisElement> marksOrModelsList();
}