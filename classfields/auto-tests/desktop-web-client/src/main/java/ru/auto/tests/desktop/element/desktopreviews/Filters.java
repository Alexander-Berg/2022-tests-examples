package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.MMMFilter;

public interface Filters extends VertisElement, WithSelect {

    @Name("Категория «{{ category }}»")
    @FindBy(".//span[contains(@class, 'CategorySwitcher')]//label[. = '{{ category }}']")
    VertisElement categorySwitcher(@Param("category") String category);

    @Name("Фильтр по марке, модели, модификации")
    @FindBy(".//div[contains(@class, 'BreadcrumbsFilter')] | " +
            ".//div[@class = 'MMMMultiFilter-module__MMMMultiFilter']")
    MMMFilter mmmFilter();

    @Name("Кнопка «Добавить отзыв»")
    @FindBy(".//a[.= 'Добавить отзыв']")
    VertisElement addReviewButton();
}