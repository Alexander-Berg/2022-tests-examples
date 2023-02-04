package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import java.sql.Timestamp;
import java.time.LocalDate;

import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFI;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - продление 7 дней")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class Prolong7daysTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private String EXPIRE_DATE;

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
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        EXPIRE_DATE = Long.toString(Timestamp.valueOf(LocalDate.now().plusDays(7).atTime(16, 0)).getTime());
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение баннера «Продление выключено»")
    public void shouldSeeAutoProlongFailedBanner() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableFalse", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().autoProlongFailedBanner().should(isDisplayed());
        basePageSteps.onCardPage().autoProlongFailedBanner().title().should(hasText("Продление выключено"));
        basePageSteps.onCardPage().autoProlongFailedBanner().text().should(anyOf(hasText("Включите продление, иначе через " +
                "7 дней объявление будет снято с публикации."), hasText("Включите продление, иначе через 8 дней " +
                "объявление будет снято с публикации.")));
        basePageSteps.onCardPage().autoProlongFailedBanner().retryButton().should(hasText("Включить продление"));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активация автопродления")
    public void shouldActivateAutoProlong() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableFalse", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().autoProlongInfo().should(anyOf(hasText("7 дней до снятия\n1 999 \u20BD / 7 дней"),
                hasText("8 дней до снятия\n1 999 \u20BD / 7 дней")));
        mockRule.newMock()
                .with("desktop/OfferCarsActivateProlongation")
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .post();

        basePageSteps.onCardPage().autoProlongFailedBanner().retryButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().autoProlongInfo().should(hasText("7 дней до продления\n1 999 \u20BD / 7 дней"));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение баннера «Проверьте кошелёк»")
    public void shouldSeeCheckWalletBanner() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().checkWalletBanner().should(isDisplayed());
        basePageSteps.onCardPage().checkWalletBanner().title().should(hasText("Проверьте кошелёк"));
        basePageSteps.onCardPage().checkWalletBanner().text().should(hasText("Если в кошельке недостаточно денег для " +
                "продления, объявления будут сняты в конце их срока действия."));
        basePageSteps.onCardPage().checkWalletBanner().link().should(hasText("Подробнее"));
        basePageSteps.onCardPage().checkWalletBanner().closeButton().should(isDisplayed());
        basePageSteps.onCardPage().checkWalletBanner().addFundsButton().should(hasText("Пополнить кошелёк"));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке закрытия баннера «Проверьте кошелёк»")
    public void shouldCloseWalletBanner() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().checkWalletBanner().should(isDisplayed());
        basePageSteps.onCardPage().checkWalletBanner().closeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().checkWalletBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке Подробнее баннера «Проверьте кошелёк»")
    public void shouldClickMoreDetailsBanner() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().checkWalletBanner().link().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DEALER).path(PROFI).shouldNotSeeDiff();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Пополнить кошелёк»")
    public void shouldClickAddFunds() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .with("desktop/SessionAuthUser")
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().checkWalletBanner().addFundsButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MY).path(WALLET).shouldNotSeeDiff();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа при наведении на «количество дней до продления»")
    public void shouldSeePopup() {
        mockRule.newMock()
                .with(mockRule.setOfferExpireDate("desktop/OfferCarsUsedUserOwnerProlongableTrue", EXPIRE_DATE))
                .post();

        cookieSteps.setCookieForBaseDomain("calls-promote-closed", "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().autoProlongInfo().timeLeft().waitUntil(isDisplayed()).hover();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed()).should(hasText("Размещение на 7 дней\n" +
                "Стоимость размещения стала ниже! Экономьте на быстрой продаже — каждую " +
                "неделю объявление будет автоматически обновляться и продлеваться за 1 999 ₽.\n" +
                "Подробнее"));
        basePageSteps.onCardPage().activePopupLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DEALER).path(PROFI).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активация неактивного объявления со скидкой")
    public void shouldActivateSaleWithDiscount() {
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwnerInactiveProlongableTrue",
                "desktop/BillingAutoruPaymentInitAutoProlong",
                "desktop/BillingAutoruPaymentProcessAutoProlong",
                "desktop/BillingAutoruPayment",
                "desktop/UserTransactionProlongablePut").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().autoProlongDiscountBanner().title()
                .should(hasText("Продлите размещение со скидкой 90%"));
        basePageSteps.onCardPage().autoProlongDiscountBanner().priceInfo()
                .should(hasText("Стоимость продления\n10 ₽\n1 ₽\nВыгода 90%"));
        basePageSteps.onCardPage().autoProlongDiscountBanner().buttonContains("Продлить за").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().waitUntil(hasText("Активация объявления"));
        basePageSteps.onCardPage().billingPopup().priceHeader().waitUntil(hasText("1 ₽"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление успешно активировано"));
    }
}
