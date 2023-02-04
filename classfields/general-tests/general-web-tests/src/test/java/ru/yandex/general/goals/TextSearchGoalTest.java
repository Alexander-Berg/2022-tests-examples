package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_RESULTS_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;

@Epic(GOALS_FEATURE)
@DisplayName("Цели текстового поиска")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TextSearchGoalTest {

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
    public String title;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Текстовый поиск", "/?region_id=213&text=телевизор"},
                {"Текстовый поиск с уточнением по категории", "/moskva/elektronika/televizori/?text=телевизор"},
                {"Текстовый поиск с уточнением по координатам", "/?geo_radius=5000&lat=55.820535&lon=37.543406&region_id=213&text=телевизор"},
                {"Текстовый поиск с уточнением по метро", "/?metro_id=20480&region_id=213&text=телевизор"},
                {"Текстовый поиск с уточнением по району", "/?district_id=117066&region_id=213&text=телевизор"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().uri(url).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_RESULTS_SHOW)
    @DisplayName("Цель «SEARCH_RESULTS_SHOW» при текстовом поиске")
    public void shouldSeeSearchResultsShowGoalOnTextSearch() {
        goalsSteps.withGoalType(SEARCH_RESULTS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(LISTING_OFFERS_SHOW)
    @DisplayName("Нет цели «LISTING_OFFERS_SHOW» при текстовом поиске")
    public void shouldNotSeeListingOffersShowGoalOnTextSearch() {
        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
