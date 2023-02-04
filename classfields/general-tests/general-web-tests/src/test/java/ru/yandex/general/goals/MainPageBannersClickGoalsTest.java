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
import static ru.yandex.general.consts.Goals.AUTO_MAINPAGE_BANNER_CLICK;
import static ru.yandex.general.consts.Goals.REALTY_MAINPAGE_BANNER_CLICK;
import static ru.yandex.general.consts.Owners.ALEXANDERREX;
import static ru.yandex.general.page.ListingPage.AUTO;
import static ru.yandex.general.page.ListingPage.REALTY;

@Epic(GOALS_FEATURE)
@Feature("Цели при переходе по баннерам Авто, Недвижимость с главной")
@DisplayName("Отправка целей при переходе по баннерам Авто и Недвижимость с главной страницы")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPageBannersClickGoalsTest {

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
    public String bannerName;

    @Parameterized.Parameter(1)
    public String bannerGoal;

    @Parameterized.Parameters(name = "{index}. Баннер {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {AUTO, AUTO_MAINPAGE_BANNER_CLICK},
                {REALTY, REALTY_MAINPAGE_BANNER_CLICK},
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @DisplayName("Отправка целей при переходе по баннерам Авто и Недвижимости")
    public void shouldSeeGoalMainPageBannerClick() {
        basePageSteps.onListingPage().banner(bannerName).click();

        goalsSteps.withGoalType(bannerGoal)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
