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

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.DISABLE_DELIVERY;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.QueryParams.DISABLE_DELIVERY_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TRUE_VALUE;
import static ru.yandex.general.element.Filters.FIND_IN_MY_REGION;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтр «Искать в моем регионе»")
@DisplayName("Тесты на фильтр «Искать в моем регионе»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class DisableDeliveryFilterTest {

    private static final String MAX_PRICE = "300000";
    private static final String MIN_PRICE = "1";
    private static final String CATEGORY_FILTER = "Оперативная память";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI);
        basePageSteps.resize(375, 1500);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Активируем свитчер «Искать в моем регионе»")
    public void shouldActivateSwitcherFindInMyRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(FIND_IN_MY_REGION).switcher().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        urlSteps.path(DISABLE_DELIVERY).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Деактивируем свитчер «Искать в моем регионе»")
    public void shouldDeactivateSwitcherFindInMyRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(DISABLE_DELIVERY).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(FIND_IN_MY_REGION).switcher().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Для региона «Россия» не отображается свитчер «Искать в моем регионе»")
    public void shouldSeeNoSwitcherFindInMyRegionInRussia() {
        urlSteps.testing().path(ROSSIYA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().searchBar().filters().click();

        basePageSteps.onListingPage().filters().filterBlock(FIND_IN_MY_REGION).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем 3 фильтра и сортировку, активируем «Искать в моем регионе», проверяем URL")
    public void shouldSeeUrlAfterSetThreeFiltersAndFindInMyRegionChecbox() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().filters().checkboxWithLabel("Новый").click();
        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(CATEGORY_FILTER).click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).item("8 ГБ").click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).button(DONE).click();
        basePageSteps.onListingPage().filters().checkboxWithLabel("Сначала дешевле").click();
        basePageSteps.onListingPage().filters().filterBlock(FIND_IN_MY_REGION).switcher().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        urlSteps.path(STATE_NEW).queryParam(DISABLE_DELIVERY_PARAM, TRUE_VALUE)
                .queryParam("offer.attributes.operativnaya-pamyat_15938685_serJvE", "8-gb")
                .queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE)
                .queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE).shouldNotDiffWithWebDriverUrl();
    }

}
