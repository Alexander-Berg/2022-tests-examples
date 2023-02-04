package ru.auto.tests.mobile.vas;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Скидочный поп-ап")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DiscountPopupTest {

    private static final String COOKIE = "discount-popup";

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
    private CookieSteps cookieSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SessionAuthUser",
                "mobile/BillingServicesDiscountCars").post();

        cookieSteps.deleteCookie(COOKIE);
        urlSteps.testing().open();
        basePageSteps.onMainPage().discountPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeePopup() {
        basePageSteps.onMainPage().discountPopup().should(hasText(matchesPattern("Скидки до 70% на опции " +
                "продвижения\nДо окончания акции .+ (день|дня|дней), \\d+ (час|часа|часов)\nТурбо\n20 просмотров\n" +
                "Ваше предложение увидит максимум посетителей — это увеличит шансы на быструю и выгодную продажу. " +
                "Объявление будет выделено цветом, поднято в топ, размещено в специальном блоке на главной странице, " +
                "на странице марки и в выдаче объявлений. Действует 3 дня.\n897 ₽-70%\n" +
                "Скидка действует\n.+ (день|дня|дней), \\d+ (час|часа|часов)\n270 ₽")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.onMainPage().discountPopup().closeIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().discountPopup().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(COOKIE, "closed");
        basePageSteps.refresh();
        basePageSteps.onMainPage().discountPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        mockRule.with("mobile/BillingAutoruPaymentInitDiscount",
                "mobile/BillingAutoruPaymentProcessDiscount",
                "desktop/BillingAutoruPayment").update();

        basePageSteps.onMainPage().discountPopup().buyButton().click();
        basePageSteps.onMainPage().paymentMethodsFrame().waitUntil(isDisplayed());
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        basePageSteps.onMainPage().discountPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MY).path(CARS).shouldNotSeeDiff();
    }
}
