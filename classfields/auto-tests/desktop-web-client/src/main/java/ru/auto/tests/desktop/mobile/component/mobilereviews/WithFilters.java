package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Filters;

public interface WithFilters {

    @Name("Фильтры")
    @FindBy(".//div[@class = 'Filters'] | " +
            ".//div[@class = 'ReviewBreadcrumbsFilters']")
    Filters filters();
}