package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.searchHistory.SearchHistoryItem;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface WithShowMoreLink {

    @Name("Линк «Мои поиски»")
    @FindBy("//span[contains(@class, 'Link')][contains(., 'Мои поиски')]")
    AtlasWebElement mySearches();

    @Name("Список предыдущих поисков")
    @FindBy("//li[@class='SearchHistoryList__item']")
    ElementsCollection<SearchHistoryItem> searchHistoryList();

    @Name("Контент «Моих поисков»")
    @FindBy("//div[contains(@class,'SearchHistory__dropdown')]")
    AtlasWebElement mySearchesContent();
}
