package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.AUTO_SEARCH_RESULTS_SNIPPET_SHOW;
import static ru.yandex.general.consts.Goals.REALTY_SEARCH_RESULTS_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.TEXT_SEARCH_TEMPLATE;
import static ru.yandex.general.mock.MockSearch.mockSearch;
import static ru.yandex.general.mock.MockWizardResponse.mockAutoExample;
import static ru.yandex.general.mock.MockWizardResponse.mockRealtyExample;
import static ru.yandex.general.step.BasePageSteps.FALSE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(GOALS_FEATURE)
@DisplayName("Отправка целей AUTO_SEARCH_RESULTS_SNIPPET_SHOW/REALTY_SEARCH_RESULTS_SNIPPET_SHOW")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class AutoAndRealtyResultsSnippetShowGoalTest {

    private static final String TEXT = "поисковый текст";
    private static final int GENERAL_OFFERS_COUNT = 5;
    private static final int WIZARD_OFFERS_COUNT_FOR_DISPLAY = 10;
    private static final int WIZARD_OFFERS_COUNT_NOT_DISPLAY = 9;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;


    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_SHOW)
    @DisplayName("Отправляется цель на авто выдаче")
    public void shouldSeeAutoSearchResultsSnippetShow() {
        mockRule.graphqlStub(mockResponse().setSearch(mockSearch(TEXT_SEARCH_TEMPLATE).addOffers(GENERAL_OFFERS_COUNT)
                        .setRequestText(TEXT).build()).build())
                .wizardStub(mockAutoExample().addAutoItems(WIZARD_OFFERS_COUNT_FOR_DISPLAY).build())
                .withDefaults().create();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_SHOW)
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(AUTO_SEARCH_RESULTS_SNIPPET_SHOW)
    @DisplayName("Не отправляется цель на выдаче недвижимости")
    public void shouldSeeNoAutoSearchResultsSnippetShow() {
        mockRule.graphqlStub(mockResponse().setSearch(mockSearch(TEXT_SEARCH_TEMPLATE).addOffers(GENERAL_OFFERS_COUNT)
                        .setRequestText(TEXT).build()).build())
                .wizardStub(mockRealtyExample().addRealtyItems(WIZARD_OFFERS_COUNT_FOR_DISPLAY).build())
                .withDefaults().create();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(AUTO_SEARCH_RESULTS_SNIPPET_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_SHOW)
    @DisplayName("Отправляется цель с на выдаче недвижимости")
    public void shouldSeeRealtySearchResultsSnippetShowWithTrueBody() {
        mockRule.graphqlStub(mockResponse().setSearch(mockSearch(TEXT_SEARCH_TEMPLATE).addOffers(GENERAL_OFFERS_COUNT)
                        .setRequestText(TEXT).build()).build())
                .wizardStub(mockRealtyExample().addRealtyItems(WIZARD_OFFERS_COUNT_FOR_DISPLAY).build())
                .withDefaults().create();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_SHOW)
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(REALTY_SEARCH_RESULTS_SNIPPET_SHOW)
    @DisplayName("Не отправляется цель на авто выдаче")
    public void shouldSeeNoRealtySearchResultsSnippetShowOnAutoListing() {
        mockRule.graphqlStub(mockResponse().setSearch(mockSearch(TEXT_SEARCH_TEMPLATE).addOffers(GENERAL_OFFERS_COUNT)
                        .setRequestText(TEXT).build()).build())
                .wizardStub(mockAutoExample().addAutoItems(WIZARD_OFFERS_COUNT_FOR_DISPLAY).build())
                .withDefaults().create();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(REALTY_SEARCH_RESULTS_SNIPPET_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
