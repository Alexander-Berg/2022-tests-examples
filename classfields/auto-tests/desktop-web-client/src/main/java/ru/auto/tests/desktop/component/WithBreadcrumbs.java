package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Breadcrumbs;
import ru.auto.tests.desktop.element.BreadcrumbsPopup;

public interface WithBreadcrumbs {

    @Name("Хлебные крошки")
    @FindBy("//div[contains(@class, 'CardBreadcrumbs')] | " +
            "//ul[contains(@class, 'BreadcrumbsGroup')]")
    Breadcrumbs breadcrumbs();

    @Name("Поп-ап хлебных крошек")
    @FindBy("//div[contains(@class,'popup_visible')] | " +
            "//div[contains(@class, 'Popup_visible')]")
    BreadcrumbsPopup breadcrumbsPopup();
}