package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SearchBar extends VertisElement {

    @Name("Открыть карту")
    @FindBy(".//button[contains(@class, 'mapButton')]")
    VertisElement mapOpen();

    @Name("Открыть поисковый экран")
    @FindBy(".//button[contains(@class, 'triggerButton')]")
    VertisElement openSearch();

    @Name("Фильтры")
    @FindBy(".//button[contains(@class, 'filtersButton')]")
    VertisElement filters();

    @Name("Сохранить поиск")
    @FindBy(".//span[contains(@class, 'saveSearchButton')]")
    VertisElement saveSearch();

}
