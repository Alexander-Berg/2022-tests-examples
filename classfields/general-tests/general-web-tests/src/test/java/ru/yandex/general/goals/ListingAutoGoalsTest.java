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
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_AUTOCHANGE_CANCEL;
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_SNIPPET_CLICK;
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_SNIPPET_SHOW;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_AUTOCHANGE_CANCEL;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_SNIPPET_CLICK;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.page.ListingPage.MORE_OFFERS_AUTORU;

@Epic(GOALS_FEATURE)
@DisplayName("Отправка целей для выдачи «Авто.Ру»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListingAutoGoalsTest {

    private static final String TEXT = "машина";

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
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Отправка «AUTO_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по снипету автошки")
    public void shouldSeeAutoSearchResultsSnippetClick() {
        basePageSteps.onListingPage().wizardSnippets().get(0).click();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Нет «REALTY_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по снипету автошки")
    public void shouldSeeNoRealtySearchResultsSnippetClick() {
        basePageSteps.onListingPage().wizardSnippets().get(0).click();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Отправка «AUTO_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по «Больше объявлений на Авто.ру»")
    public void shouldSeeAutoSearchResultsSnippetClickOnMoreButton() {
        basePageSteps.onListingPage().link(MORE_OFFERS_AUTORU).click();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
    @DisplayName("Нет «REALTY_SEARCH_RESULTS_SNIPPET_CLICK» при переходе по «Больше объявлений на Авто.ру»")
    public void shouldSeeNoRealtySearchResultsSnippetClickOnMoreButton() {
        basePageSteps.onListingPage().link(MORE_OFFERS_AUTORU).click();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_CLICK)
                .withCount(0)
                .shouldExist();
    }

}
