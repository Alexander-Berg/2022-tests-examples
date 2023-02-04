package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SidebarCategories extends VertisElement, Link {

    String ALL_CATEGORIES = "Все категории";

    @Name("Активная категория в блоке навигации")
    @FindBy(".//div[contains(@class, 'FoundCategories__category')]/div[contains(@class, 'Text__defaultColor')]")
    VertisElement activeCategory();

    @Name("Активная категория «{{ value }}» в блоке навигации")
    @FindBy(".//div[contains(@class, 'FoundCategories__category')]/div[contains(@class, 'Text__defaultColor')]" +
            "[contains(., '{{ value }}')]")
    VertisElement activeCategory(@Param("value") String value);

}
