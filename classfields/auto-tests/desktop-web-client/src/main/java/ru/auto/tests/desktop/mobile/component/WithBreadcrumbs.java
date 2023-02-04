package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.Breadcrumbs;

public interface WithBreadcrumbs {

    @Name("Хлебные крошки")
    @FindBy("//div[contains(@class, 'CardBreadcrumbs')] | " +
            "//ul[contains(@class, 'BreadcrumbsGroup')] | " +
            "//div[contains(@class, 'PageVersusMobile__breadcrumbs')]")
    Breadcrumbs breadcrumbs();
}