package ru.auto.tests.forms.reviews;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@DisplayName("Отзывы - предзаполнение отзыва")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotoAddPrefillTest {

    private static final String OFFER_ID = "1101321480-708dd3a5";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop/ReferenceCatalogCarsSuggest",
                "desktop/SearchMotoBreadcrumbsRid225",
                "desktop/SearchMotoBreadcrumbsHondaXl1000V",
                "forms/ReviewsAutoMotoOffer",
                "forms/UserReviewsReviewIdMoto").post();

        formsSteps.setWindowHeight(3000);
        urlSteps.testing().path(MOTO).path(REVIEWS).path(ADD).path(OFFER_ID)
                .addParam("campaign", "dsktp_lk_promo").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Предзаполнение отзыва мото")
    public void shouldPrefillReview() {
        formsSteps.onFormsPage().h1().hover().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        urlSteps.onCurrentUrl().setProduction().open();
        formsSteps.onFormsPage().h1().hover().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
