package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Промо отзывов после снятия с продажи")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class ReviewsPromoMotoTest {

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersMotoActive"),
                stub("desktop-lk/UserOffersMotoHide"),
                stub().withGetDeepEquals(USER_OFFERS_MOTO_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.MOTO.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(MOTO).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение промо")
    public void shouldSeePromo() {
        screenshotSteps.setWindowSizeForScreenshot();

        deactivateSale();
        basePageSteps.onLkSalesNewPage().reviewsPromo().title().hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesNewPage().reviewsPromo());

        urlSteps.onCurrentUrl().setProduction().open();
        deactivateSale();
        basePageSteps.onLkSalesNewPage().reviewsPromo().title().hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesNewPage().reviewsPromo());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по кнопке «Оставить отзыв»")
    public void shouldClickSaveButton() {
        deactivateSale();

        basePageSteps.onLkSalesNewPage().reviewsPromo().saveButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().reviewsPromo().waitUntil(not(isDisplayed()));
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MOTO).path(REVIEWS).path(ADD).path("/1076842087-f1e84/")
                .addParam("rvw_campaign", "dsktp_lk_promo").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрытие промо")
    public void shouldClosePromo() {
        deactivateSale();
        basePageSteps.onLkSalesNewPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();

        basePageSteps.onLkSalesNewPage().reviewsPromo().should(not(isDisplayed()));
    }

    @Step("Снимаем объявление с продажи")
    public void deactivateSale() {
        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesNewPage().soldPopup().button("Снять с продажи").waitUntil(isEnabled()).click();
    }

}
