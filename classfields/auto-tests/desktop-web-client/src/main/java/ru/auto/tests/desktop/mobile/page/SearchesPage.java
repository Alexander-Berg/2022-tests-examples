package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.SavedSearch;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.03.18
 */
public interface SearchesPage extends BasePage {

    @Name("Список сохраненных поисков")
    @FindBy("//div[contains(@class, 'SubscriptionItemMobile')]")
    ElementsCollection<SavedSearch> savedSearchesList();

    @Name("Заглушка")
    @FindBy("//div[@class = 'SearchesPage__empty'] | " +
            "//div[@class = 'PageSearches__empty']")
    VertisElement stub();

    default SavedSearch getSavedSearch(int i) {
        return savedSearchesList().should(hasSize(greaterThan(i))).get(i);
    }
}
