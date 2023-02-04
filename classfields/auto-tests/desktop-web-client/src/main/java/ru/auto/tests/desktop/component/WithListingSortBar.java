package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.SortBar;

public interface WithListingSortBar {

    @Name("Панель сортировок")
    @FindBy("//div[@class = 'ListingFilterPanel']")
    SortBar sortBar();
}