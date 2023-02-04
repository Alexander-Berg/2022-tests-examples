package ru.auto.tests.poffer.vas;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER_VAS_LANDING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.VAS;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Частник - лендинг VAS после редактирования объявления")
@Feature(POFFER_VAS_LANDING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasLandingTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/User"),
                stub("desktop/Currencies"),
                stub("desktop/ReferenceCatalogCarsAllOptions"),
                stub("poffer/ReferenceCatalogCarsSuggest"),
                stub("poffer/UserOffersCars"),
                stub("poffer/UserOffersCarsEdit"),
                stub("poffer/UserDraftCarsDraftIdGet"),
                stub("poffer/UserModerationStatus"),
                stub("poffer/UserDraftCarsDraftIdPut"),
                stub("poffer/UserDraftCarsDraftIdPublish")
        ).create();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение лендинга")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void shouldSeeLanding() {
        pofferSteps.setWindowHeight(8000);

        urlSteps.testing().path(VAS).path(CARS).path(OFFER_ID).open();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().vasLanding());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().vasLanding());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Продолжить без дополнительных опций»")
    public void shouldClickContinueUrl() {
        urlSteps.testing().path(VAS).path(CARS).path(OFFER_ID).open();
        pofferSteps.onPofferPage().vasLanding().button("Продолжить без дополнительных опций").click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID).shouldNotSeeDiff();
    }
}
