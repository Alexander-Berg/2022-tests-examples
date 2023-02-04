package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.history.SearchHistoryItem;

/**
 * Created by kopitsa on 22.08.17.
 */
public interface HistoryPage extends BasePage {

    @Name("Список истории поисков")
    @FindBy("//div[@class = 'search-history__item i-bem']")
    ElementsCollection<SearchHistoryItem> searchHistoryList();
}
