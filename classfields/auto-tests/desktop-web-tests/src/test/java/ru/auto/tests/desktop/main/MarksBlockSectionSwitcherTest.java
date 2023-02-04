package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - блок марок - переключатель «Все/Новые/С пробегом»")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarksBlockSectionSwitcherTest {

    private static final String FIRST_MARK = "vaz";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String startSection;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String sectionUrl;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Новые", "Все", ALL},
                {"Все", "Новые", NEW},
                {"Все", "С пробегом", USED}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsMarkModelFiltersAll",
                "desktop/SearchCarsMarkModelFiltersNew",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices",
                "desktop/SearchCarsMarkModelFiltersUsed").post();

        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().marksBlock().switcher(startSection).click();
        basePageSteps.onMainPage().marksBlock().switcher(section).click();
        basePageSteps.onMainPage().marksBlock().selectedSwitcher(section).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока")
    public void shouldSeeMarksBlock() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onMainPage().marksBlock(),
                        newHashSet(basePageSteps.onMainPage().marksBlock().banner()));

        urlSteps.setProduction().open();
        basePageSteps.onMainPage().marksBlock().switcher(startSection).click();
        basePageSteps.onMainPage().marksBlock().switcher(section).click();
        basePageSteps.onMainPage().marksBlock().selectedSwitcher(section).waitUntil(isDisplayed());
        Screenshot prodScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onMainPage().marksBlock(),
                        newHashSet(basePageSteps.onMainPage().marksBlock().banner()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, prodScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по логотипу марки")
    public void shouldClickMarkLogo() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onMainPage().marksBlock().getMarkLogo(0).click();
        urlSteps.path(CARS).path(FIRST_MARK).path(sectionUrl).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по популярной марке")
    public void shouldClickMark() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onMainPage().marksBlock().getMark(0).click();
        urlSteps.path(CARS).path(FIRST_MARK).path(sectionUrl).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Показать»")
    public void shouldClickResultsButton() {
        mockRule.with("desktop/ProxyPublicApi").update();

        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(sectionUrl).shouldNotSeeDiff();
    }
}
