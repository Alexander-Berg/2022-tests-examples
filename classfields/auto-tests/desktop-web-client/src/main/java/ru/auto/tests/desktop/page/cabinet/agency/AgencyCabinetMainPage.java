package ru.auto.tests.desktop.page.cabinet.agency;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.agency.ExpensesGraph;
import ru.auto.tests.desktop.element.cabinet.agency.Listing;
import ru.auto.tests.desktop.element.cabinet.agency.ListingFilters;
import ru.auto.tests.desktop.element.cabinet.agency.WithPopup;
import ru.auto.tests.desktop.element.cabinet.agency.header.Header;
import ru.auto.tests.desktop.page.cabinet.CabinetOffersPage;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.09.18
 */
public interface AgencyCabinetMainPage extends CabinetOffersPage, WithPopup, WithNotifier {

    @Name("Шапка")
    @FindBy("//div[contains(@class, 'Header__container')]")
    Header header();

    @Name("График расходов")
    @FindBy("//div[.//div[contains(@class, 'dashboard-agency-graph')] and contains(@class, 'dashboard-agency__cell')] | " +
            "//div[contains(@class, 'DashboardAgencyCharts')]")
    ExpensesGraph expensesGraph();

    @Name("Круговая диаграмма")
    @FindBy("//div[.//div[@class = 'dashboard-summery__chart-data'] and contains(@class, 'dashboard-agency__cell')] | " +
            "//div[contains(@class, 'DashboardAgencyCharts__pieChart')]")
    VertisElement pieChart();

    @Name("Фильтры листингов")
    @FindBy("//div[contains(@class, 'listing-new')]/div[contains(@class, 'listing-filter-new')] | " +
            "//div[@class = 'FilterPresets'] | //div[@class = 'ClientsFilters']")
    ListingFilters listingFilters();

    @Name("Листинги")
    @FindBy("//div[@class = 'listing-items-list']/div[contains(@class, 'listing-item-new')] | " +
            "//div[@class = 'Clients__listing']/div[contains(@class, 'ClientsItem')]")
    ElementsCollection<Listing> listingList();

    @Name("Список клиентов")
    @FindBy("//div[@class = 'ClientsItem']")
    ElementsCollection<Listing> clientsList();


}
