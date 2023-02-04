package ru.auto.tests.desktop.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.VAS_DISCOUNT;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Скидочный поп-ап")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DiscountPopupTest {

    private static final String POPUP_COOKIE_NAME = "discount-popup";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SessionAuthUser",
                "desktop/BillingServicesDiscountAll",
                "desktop/BillingAutoruPaymentInitDiscountPopup",
                "desktop/BillingAutoruPaymentProcessDiscountPopup",
                "desktop/BillingAutoruPayment").post();

        cookieSteps.deleteCookie(POPUP_COOKIE_NAME);
        urlSteps.testing().addParam(FORCE_POPUP, VAS_DISCOUNT).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeePopup() {
        basePageSteps.onMainPage().discountPopup().waitUntil(isDisplayed()).should(hasText("Тотальная распродажа\n25%\n" +
                "скидка на опции продвижения объявления\nСтикеры быстрой продажи\n×2 просмотров\n?\n30 ₽\n99 ₽\n" +
                "Включить\nТурбо\n×20 просмотров\n?\n210 ₽\n697 ₽\nВключить\nЭкспресс\n×5 просмотров\n?\n120 ₽\n397 ₽\n" +
                "Включить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onMainPage().discountPopup().closeIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().discountPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().discountTimer().discount().waitUntil(hasText("25%"));
        cookieSteps.shouldSeeCookieWithValue(POPUP_COOKIE_NAME, "closed");
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги")
    public void shouldBuyService() {
        basePageSteps.onMainPage().discountPopup().turnOnButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().switchToBillingFrame();
        basePageSteps.onMainPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().billingPopup().checkbox("Запомнить карту").click();
        basePageSteps.onMainPage().billingPopup().header().waitUntil(hasText("Стикеры быстрой продажи"));
        basePageSteps.onCardPage().billingPopup().priceHeader().waitUntil(hasText("1 497 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onMainPage().notifier().waitUntil(isDisplayed()).should(hasText("Опция " +
                "«Стикеры быстрой продажи» подключена"));
        basePageSteps.onMainPage().billingPopupCloseButton().click();

        basePageSteps.onMainPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().notifier().waitUntil(not(isDisplayed()));
    }
}
