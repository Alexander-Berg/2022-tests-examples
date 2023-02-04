package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Промо отзывов после снятия с продажи")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsPromoTrucksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersTrucksActive"),
                stub("desktop-lk/UserOffersTrucksHide"),
                stub().withGetDeepEquals(USER_OFFERS_TRUCKS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.TRUCKS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(TRUCKS).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение промо")
    public void shouldSeePromo() {
        screenshotSteps.setWindowSizeForScreenshot();

        deactivateSale();
        basePageSteps.onLkSalesPage().reviewsPromo().title().hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().reviewsPromo());

        urlSteps.onCurrentUrl().setProduction().open();
        deactivateSale();
        basePageSteps.onLkSalesPage().reviewsPromo().title().hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().reviewsPromo());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Оставить отзыв»")
    public void shouldClickSaveButton() {
        deactivateSale();

        basePageSteps.onLkSalesPage().reviewsPromo().saveButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().reviewsPromo().waitUntil(not(isDisplayed()));
        urlSteps.switchToNextTab();
        urlSteps.testing().path(TRUCKS).path(REVIEWS).path(ADD).path("/1076842087-f1e84/")
                .addParam("rvw_campaign", "dsktp_lk_promo").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие промо")
    public void shouldClosePromo() {
        deactivateSale();

        basePageSteps.onLkSalesPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().reviewsPromo().waitUntil(not(isDisplayed()));
    }

    @Step("Снимаем объявление с продажи")
    public void deactivateSale() {
        basePageSteps.onLkSalesPage().getSale(0).button("Снять с продажи").click();
        basePageSteps.onLkSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesPage().soldPopup().button("Снять с продажи").waitUntil(isEnabled()).click();
    }

}
