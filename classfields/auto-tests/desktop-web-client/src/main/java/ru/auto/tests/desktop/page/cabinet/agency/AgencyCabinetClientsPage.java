package ru.auto.tests.desktop.page.cabinet.agency;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.cabinet.agency.Listing;
import ru.auto.tests.desktop.element.cabinet.agency.ListingFilters;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 20.09.18
 */
public interface AgencyCabinetClientsPage extends WebPage, WithPager {

    @Name("Фильтры листингов")
    @FindBy("//div[@class = 'FilterPresets'] | //div[@class = 'ClientsFilters']")
    ListingFilters listingFilters();

    @Name("Список клиентов")
    @FindBy("//div[@class = 'ClientsItem']")
    ElementsCollection<Listing> clientsList();

    @Name("Листинги")
    @FindBy("//div[@class = 'listing-items-list']/div[contains(@class, 'listing-item-new')]")
    ElementsCollection<Listing> listingList();

    @Step("Все статусы объявлений должны быть «{status}»")
    default void everyStatusesIs(String status) {
        List<String> statuses = listingList().stream().map(e -> e.status().getText()).collect(Collectors.toList());
        assertThat(statuses, everyItem(is(status)));
    }
}
