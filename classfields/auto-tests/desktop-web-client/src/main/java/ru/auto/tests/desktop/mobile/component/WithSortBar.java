package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.listing.SortBar;
import ru.auto.tests.desktop.mobile.element.listing.SortPopup;

public interface WithSortBar {

    @Name("Панель сортировок")
    @FindBy("//div[contains(@class, 'ListingSortTabs')] |" +
            "//div[contains(@class, 'sortingTabs')]")
    SortBar sortBar();

    @Name("Поп-ап сортировок")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    SortPopup sortPopup();
}