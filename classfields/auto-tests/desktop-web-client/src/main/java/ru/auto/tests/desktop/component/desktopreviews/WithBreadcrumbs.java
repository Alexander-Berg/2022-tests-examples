package ru.auto.tests.desktop.component.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.desktopreviews.Breadcrumbs;

public interface WithBreadcrumbs {

    @Name("Хлебные крошки в легковых")
    @FindBy("//div[contains(@class, 'BreadcrumbsPanel')]")
    Breadcrumbs breadcrumbs();
}