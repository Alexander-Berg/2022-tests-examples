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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятие с продажи")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class DeactivateSaleCarsTest {

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
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsActive"),
                stub("desktop-lk/UserOffersCarsStats")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение поп-апа снятия с продажи (с подключёнными услугами)")
    public void shouldSeeSoldPopup() {
        screenshotSteps.setWindowSizeForScreenshot();

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(3, basePageSteps.onLkSalesNewPage().soldPopup());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(3, basePageSteps.onLkSalesNewPage().soldPopup());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsInactive"),
                stub("desktop-lk/UserOffersCarsHide"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();

        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Снятие с продажи с указанием цены продажи")
    public void shouldDeactivateSaleSoldPrice() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsInactive"),
                stub("desktop-lk/UserOffersCarsPredictBuyers"),
                stub("desktop-lk/UserOffersCarsHideSoldPrice"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesNewPage().soldPopup().input("Стоимость, \u20BD", "500000");
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Снятие с продажи - выбор номера телефона покупателя из списка")
    public void shouldDeactivateSaleBuyersPhoneFromList() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsInactive"),
                stub("desktop-lk/UserOffersCarsPredictBuyers"),
                stub("desktop-lk/UserOffersCarsHideBuyerPhone"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("+7 911 111-11-1110 июля 2020 в 17:26").click();
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Снятие с продажи - другой номера телефона покупателя")
    public void shouldDeactivateSaleBuyersOtherPhone() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsInactive"),
                stub("desktop-lk/UserOffersCarsPredictBuyers"),
                stub("desktop-lk/UserOffersCarsHideBuyerPhone"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Другой номер").click();
        basePageSteps.onLkSalesNewPage().soldPopup().input("Введите телефон покупателя", "+79111111111");
        deactivate();
    }

    @Step("Снимаем объявление с продажи")
    private void deactivate() {
        basePageSteps.onLkSalesNewPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesNewPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().reviewsPromo().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesNewPage().getSale(0).status().waitUntil(hasText("Снято с продажи"));

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).activateButton().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().getSale(0).button("Опубликовать объявление").should(isDisplayed());
    }

}
