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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.SEARCH_RESULTS_EMPTY_MORE_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_RESULTS_EMPTY_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цели SEARCH_RESULTS_EMPTY_SHOW и SEARCH_RESULTS_EMPTY_MORE_SHOW")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class SearchResultsEmptyGoalsTest {

    private static final String DNO_REGION_ID = "10929";
    private static final String ABRAKADABRA = "safjaskfgqjgqkwjfaksjfasafsafafjhqiwhgi";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_MORE_SHOW)
    @DisplayName("Цель «SEARCH_RESULTS_EMPTY_MORE_SHOW» при открытии пустой выдачи с блоком «Предложения в других регионах»")
    public void shouldSeeSearchResultsEmptyMoreShowGoal() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, DNO_REGION_ID);
        urlSteps.testing().queryParam(TEXT_PARAM, "переноска").queryParam(REGION_ID_PARAM, DNO_REGION_ID)
                        .queryParam(LATITUDE_PARAM, "57.833448").queryParam(LONGITUDE_PARAM, "29.969595")
                        .queryParam(GEO_RADIUS_PARAM, "1000").open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_MORE_SHOW)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_SHOW» при открытии пустой выдачи с блоком «Предложения в других регионах»")
    public void shouldSeeNoSearchResultsEmptyShowGoal() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, DNO_REGION_ID);
        urlSteps.testing().queryParam(TEXT_PARAM, "переноска").queryParam(REGION_ID_PARAM, DNO_REGION_ID)
                .queryParam(LATITUDE_PARAM, "57.833448").queryParam(LONGITUDE_PARAM, "29.969595")
                .queryParam(GEO_RADIUS_PARAM, "1000").open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_SHOW)
    @DisplayName("Цель «SEARCH_RESULTS_EMPTY_SHOW» при открытии пустой выдачи без блока «Предложения в других регионах»")
    public void shouldSeeSearchResultsEmptyShowGoal() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, ABRAKADABRA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_SHOW)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_MORE_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_MORE_SHOW» при открытии пустой выдачи без блока «Предложения в других регионах»")
    public void shouldSeeNoSearchResultsEmptyMoreShowGoal() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, ABRAKADABRA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_MORE_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_SHOW» при открытии листинга")
    public void shouldSeeNoSearchResultsEmptyShowGoalOnListing() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_MORE_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_MORE_SHOW» при открытии листинга")
    public void shouldSeeNoSearchResultsEmptyMoreShowGoalOnListing() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_MORE_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_SHOW» при открытии текстового поиска")
    public void shouldSeeNoSearchResultsEmptyShowGoalOnTextSearch() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, "ноутбук").open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_MORE_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_MORE_SHOW» при открытии текстового поиска")
    public void shouldSeeNoSearchResultsEmptyMoreShowGoalOnTextSearch() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, "ноутбук").open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_MORE_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_SHOW» при открытии главной")
    public void shouldSeeNoSearchResultsEmptyShowGoalOnHomepage() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_EMPTY_MORE_SHOW)
    @DisplayName("Нет цели «SEARCH_RESULTS_EMPTY_MORE_SHOW» при открытии главной")
    public void shouldSeeNoSearchResultsEmptyMoreShowGoalOnHomepage() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).open();

        goalsSteps.withGoalType(SEARCH_RESULTS_EMPTY_MORE_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
