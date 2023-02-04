package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SORTING_FEATURE;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_DATE_DESC_CLICK;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_PRICE_ASC_CLICK;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_PRICE_DESC_CLICK;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PUBLISH_DATE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(GOALS_FEATURE)
@Feature(SORTING_FEATURE)
@DisplayName("Отправка цели сортировки при выборе каждого типа сортировки")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingOffersSortGoalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String sortName;

    @Parameterized.Parameter(1)
    public String sortParamValue;

    @Parameterized.Parameter(2)
    public String sortGoal;

    @Parameterized.Parameters(name = "Сортировка «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"По актуальности", SORT_BY_RELEVANCE_VALUE, LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK},
                {"Сначала свежие", SORT_BY_PUBLISH_DATE_DESC_VALUE, LISTING_OFFERS_SORT_DATE_DESC_CLICK},
                {"Сначала дешевле", SORT_BY_PRICE_ASC_VALUE, LISTING_OFFERS_SORT_PRICE_ASC_CLICK},
                {"Сначала дороже", SORT_BY_PRICE_DESC_VALUE, LISTING_OFFERS_SORT_PRICE_DESC_CLICK}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка цели сортировки при выборе каждого типа сортировки")
    public void shouldSeeSortGoalWithEverySortType() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.resize(600, 1500);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().spanLink(sortName).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(sortGoal)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

}
