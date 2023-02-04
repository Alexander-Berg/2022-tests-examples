package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - групповой сниппет")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SnippetNewCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("mobile/SearchCarsNew"),
                stub("desktop/ReferenceCatalogCarsDictionariesV1Equipment")).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение группового сниппета")
    public void shouldSeeSnippet() {
        basePageSteps.setWindowHeight(6000);
        basePageSteps.refresh();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onListingPage().getGroupSale(0));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onListingPage().getGroupSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по групповому сниппету")
    public void shouldClickGroupSnippet() {
        mockRule.setStubs(stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")).update();

        basePageSteps.onListingPage().getGroupSale(1).hover();
        basePageSteps.onListingPage().getGroupSale(1).header().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path("/kia/optima/20526471-20526494/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «N предложений»")
    public void shouldClickShowOffersButton() {
        basePageSteps.onListingPage().getGroupSale(1).showOffersButton().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().getGroupSale(1).showOffersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path("/kia/optima/20526471-20526494/").shouldNotSeeDiff();
    }
}
