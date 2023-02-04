package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активация похожего оъявления")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@Ignore
@GuiceModules(DesktopTestsModule.class)
public class ActivateSameSaleCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219, FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/UserOffersCarsInactive"),
                stub("desktop-lk/UserOffersCarsActivateSameSale"),
                stub("desktop-lk/UserOffersCarsIdSameSale"),
                stub("desktop-lk/BillingAutoruPaymentInitSameSale"),
                stub("desktop-lk/BillingAutoruPaymentProcess"),
                stub("desktop-lk/BillingAutoruPayment"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активация")
    public void shouldActivate() {
        basePageSteps.onLkSalesNewPage().getSale(0).button("Опубликовать объявление").click();
        basePageSteps.onLkSalesNewPage().popup().waitUntil(hasText("Повторное размещение\n" +
                "Вы уже размещали объявление о продаже этого авто недавно. Повторное размещение на 60 дней " +
                "стоит 1 199 ₽. Старое объявление вы можете восстановить бесплатно.\nВосстановить старое\n" +
                "Разместить за 1 199 ₽\nLifan Solano\n2020 г, 500 км\nСедан, белый\n1.5 MT (100 л.с.)\nбензин, " +
                "передний\nМосква\n, 3 июня 2020\n500 000 ₽"));
        basePageSteps.onLkSalesNewPage().popup().button("Разместить за 1\u00a0199\u00a0₽").click();
        basePageSteps.onLkSalesNewPage().switchToBillingFrame();
        basePageSteps.onLkSalesNewPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().billingPopup().header().waitUntil(hasText("Активация объявления"));
        basePageSteps.onLkSalesNewPage().billingPopup().priceHeader().waitUntil(hasText("1 199 ₽"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Объявление активировано"));
        basePageSteps.onLkSalesNewPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesNewPage().header().logo().hover();
        basePageSteps.onLkSalesNewPage().getSale(0).button("Развернуть").click();

        basePageSteps.onLkSalesNewPage().getSale(0).chart().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().getSale(0).vas().waitUntil(isDisplayed());
    }
}
