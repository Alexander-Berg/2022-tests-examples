package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.Pages.STATE_USED;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.element.FiltersPopup.BU;
import static ru.yandex.general.mobile.element.FiltersPopup.NOVIY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры состояния")
@DisplayName("Фильтры. Состояние")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class ConditionFilterTest {

    private static final String OFFER_STATE = "offer.state";
    private static final String CATEGORY_PATH = NOUTBUKI;
    private static final String NEW = "new";
    private static final String USED = "used";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH);
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Б/У» видим в урле")
    public void shouldSeeUsedFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(BU).waitUntil(isDisplayed());
        urlSteps.path(STATE_USED).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Новый» видим в урле")
    public void shouldSeeNewFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(NOVIY).waitUntil(isDisplayed());
        urlSteps.path(STATE_NEW).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Новый + Б/У» видим в урле")
    public void shouldSeeNewUsedFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s, %s", NOVIY, BU)).waitUntil(isDisplayed());
        urlSteps.queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «Б/У»")
    public void shouldSeeCheckedUsedFilter() {
        urlSteps.path(STATE_USED).open();
        basePageSteps.onListingPage().filter(BU).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «Новый»")
    public void shouldSeeCheckedNewFilter() {
        urlSteps.path(STATE_NEW).open();
        basePageSteps.onListingPage().filter(NOVIY).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «Новый + Б/У»")
    public void shouldSeeCheckedNewUsedFilter() {
        urlSteps.queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED).open();
        basePageSteps.onListingPage().filter(format("%s, %s", NOVIY, BU)).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Скидываем фильтр «Новый + Б/У»")
    public void shouldSeeCloseConditionFilter() {
        urlSteps.queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED).open();
        basePageSteps.onListingPage().filter(format("%s, %s", NOVIY, BU)).closeFilter().click();
        basePageSteps.wait500MS();
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Закрываем попап -> фильтр не применяется")
    public void shouldSeeCancelFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().closePopup().click();
        basePageSteps.onListingPage().filter(NOVIY).should(not(isDisplayed()));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Новый» отображается в расширенных фильтрах и в урле")
    public void shouldSeeNewFilterInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(NOVIY).waitUntil(isDisplayed());
        urlSteps.path(STATE_NEW).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Б/У» отображается в расширенных фильтрах и в урле")
    public void shouldSeeUsedFilterInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(BU).waitUntil(isDisplayed());
        urlSteps.path(STATE_USED).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

}
