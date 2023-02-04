package ru.yandex.realty.mobile.element.history;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 22.08.17.
 */
public interface SearchHistoryItem extends AtlasWebElement {

    @Name("Ссылка на историю")
    @FindBy(".//a[contains(@class, 'search-history__item-title')]")
    AtlasWebElement historyLink();

    @Name("Кнопка удаления")
    @FindBy(".//div[contains(@class, 'search-history__item-remove')]")
    AtlasWebElement deleteButton();
}
