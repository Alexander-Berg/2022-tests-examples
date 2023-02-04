package ru.yandex.general.goals;

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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SORTING_FEATURE;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(GOALS_FEATURE)
@Feature(SORTING_FEATURE)
@DisplayName("Цель «LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListingOffersSortByRelevanceGoalTest {

    private static final String BY_RELEVANCE = "По актуальности";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка цели «LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK» при выборе сортировки «По актуальности»")
    public void shouldSeeSortGoalByRelevance() {
        basePageSteps.onListingPage().filters().sortButton().click();
        basePageSteps.onListingPage().popup().radioButtonWithLabel(BY_RELEVANCE).click();

        goalsSteps.withGoalType(LISTING_OFFERS_SORT_RELEVANCE_DESC_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

}
