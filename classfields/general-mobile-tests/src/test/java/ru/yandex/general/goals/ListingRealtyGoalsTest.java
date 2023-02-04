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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_AUTOCHANGE_CANCEL;
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_SNIPPET_CLICK;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_AUTOCHANGE_CANCEL;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_SNIPPET_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.page.ListingPage.MORE_OFFERS_REALTY;
import static ru.yandex.general.mobile.page.ListingPage.UNDO;

@Epic(GOALS_FEATURE)
@DisplayName("Отправка целей для выдачи «Недвижимость»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class ListingRealtyGoalsTest {

    private static final String TEXT = "квартира";

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
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Отправка «REALTY_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по снипету недвижки")
    public void shouldSeeRealtySearchResultsSnippetClick() {
        basePageSteps.onListingPage().wizardSnippets().get(0).click();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Нет «AUTO_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по снипету недвижки")
    public void shouldSeeNoAutoSearchResultsSnippetClick() {
        basePageSteps.onListingPage().wizardSnippets().get(0).click();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Отправка «REALTY_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по «Больше объявлений на Я.Недвижимости»")
    public void shouldSeeRealtySearchResultsSnippetClickOnMoreButton() {
        basePageSteps.onListingPage().link(MORE_OFFERS_REALTY).click();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Нет «AUTO_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по «Больше объявлений на Я.Недвижимости»")
    public void shouldSeeNoAutoSearchResultsSnippetClickOnMoreButton() {
        basePageSteps.onListingPage().link(MORE_OFFERS_REALTY).click();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCount(0)
                .shouldExist();
    }

}
