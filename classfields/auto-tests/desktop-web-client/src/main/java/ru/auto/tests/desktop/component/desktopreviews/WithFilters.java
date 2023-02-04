package ru.auto.tests.desktop.component.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.desktopreviews.Filters;

public interface WithFilters {

    @Name("Фильтры")
    @FindBy(".//div[contains(@class, '__form PageReview')]")
    Filters filters();
}