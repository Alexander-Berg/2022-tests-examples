package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiSavedSearchesListing;
import ru.auto.tests.publicapi.model.AutoApiSearchCatalogFilter;
import ru.auto.tests.publicapi.model.AutoApiSearchInstance;
import ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Comparator;
import java.util.List;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.MOTORCYCLE;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.LCV;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /search/history")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetSearchHistoryTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    private final String MARK_AUTO = "AUDI";
    private final String MODEL_AUTO = "100";
    private final String MARK_MOTO = "ABM";
    private final String MODEL_MOTO = "ALPHA_110";
    private final String MARK_TRUCKS = "CHEVROLET";
    private final String MODEL_TRUCKS = "EXPRESS";


    @Test
    public void shouldSee403WhenNonAuth() {
        api.search().getHistorySearch().executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeEmptyListWhenNoSearchesForUser() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiSavedSearchesListing searchHistory = api.search().getHistorySearch()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(searchHistory.getSavedSearches()).isNullOrEmpty();
    }

    @Test
    public void shouldSeeAddedSearchWithParamsInResponse() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiSearchSearchRequestParameters autoSearch = new AutoApiSearchSearchRequestParameters()
                .addCatalogFilterItem(new AutoApiSearchCatalogFilter().mark(MARK_AUTO).model(MODEL_AUTO));
        AutoApiSearchSearchRequestParameters motoSearch = new AutoApiSearchSearchRequestParameters()
                .motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(MOTORCYCLE))
                .addCatalogFilterItem(new AutoApiSearchCatalogFilter().mark(MARK_MOTO).model(MODEL_MOTO));
        AutoApiSearchSearchRequestParameters truckSearch = new AutoApiSearchSearchRequestParameters()
                .trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(LCV))
                .addCatalogFilterItem(new AutoApiSearchCatalogFilter().mark(MARK_TRUCKS).model(MODEL_TRUCKS));

        adaptor.addSearchToSearchHistory(sessionId, autoSearch, CARS.toString());
        adaptor.addSearchToSearchHistory(sessionId, motoSearch, MOTO.toString());
        adaptor.addSearchToSearchHistory(sessionId, truckSearch, TRUCKS.toString());

        List<AutoApiSearchInstance> savedSearches = api.search().getHistorySearch()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .xFeaturesHeader("search_catalog_filter_moto,search_catalog_filter_trucks,search_catalog_filter_cars")
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getSavedSearches();
        savedSearches.sort(Comparator.comparing(AutoApiSearchInstance::getCreationTimestamp));

        assertThat(savedSearches.size()).isEqualTo(3);

        assertThat(savedSearches.get(0).getCategory()).isEqualByComparingTo(CARS);
        assertThat(savedSearches.get(0).getParams().getCatalogFilter().get(0).getMark()).isEqualTo(MARK_AUTO);
        assertThat(savedSearches.get(0).getParams().getCatalogFilter().get(0).getModel()).isEqualTo(MODEL_AUTO);

        assertThat(savedSearches.get(1).getCategory()).isEqualByComparingTo(MOTO);
        assertThat(savedSearches.get(1).getParams().getMotoParams().getMotoCategory()).isEqualByComparingTo(MOTORCYCLE);
        assertThat(savedSearches.get(1).getParams().getCatalogFilter().get(0).getMark()).isEqualTo(MARK_MOTO);
        assertThat(savedSearches.get(1).getParams().getCatalogFilter().get(0).getModel()).isEqualTo(MODEL_MOTO);

        assertThat(savedSearches.get(2).getCategory()).isEqualByComparingTo(TRUCKS);
        assertThat(savedSearches.get(2).getParams().getTrucksParams().getTrucksCategory()).isEqualByComparingTo(LCV);
        assertThat(savedSearches.get(2).getParams().getCatalogFilter().get(0).getMark()).isEqualTo(MARK_TRUCKS);
        assertThat(savedSearches.get(2).getParams().getCatalogFilter().get(0).getModel()).isEqualTo(MODEL_TRUCKS);
    }

    @Test
    public void shouldNotDuplicateSimilarSearches() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiSearchSearchRequestParameters autoSearch = new AutoApiSearchSearchRequestParameters()
                .addCatalogFilterItem(new AutoApiSearchCatalogFilter().mark(MARK_AUTO).model(MODEL_AUTO));
        adaptor.addSearchToSearchHistory(sessionId, autoSearch, CARS.toString());
        adaptor.addSearchToSearchHistory(sessionId, autoSearch, CARS.toString());

        AutoApiSavedSearchesListing getSearchesHistory = api.search().getHistorySearch()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(getSearchesHistory.getSavedSearches().size()).isEqualTo(1);
    }
}
