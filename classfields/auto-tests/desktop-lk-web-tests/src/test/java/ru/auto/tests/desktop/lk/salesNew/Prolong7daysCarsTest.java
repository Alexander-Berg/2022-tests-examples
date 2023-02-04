package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDate;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFI;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Личный кабинет - продление 7 дней")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class Prolong7daysCarsTest {

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
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        EXPIRE_DATE = Long.toString(Timestamp.valueOf(LocalDate.now().plusDays(7).atTime(16, 0)).getTime());
        mockRule.newMock().with("desktop/SessionAuthUser", "desktop/UserOffersCarsCount");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение на сниппете в объявлении x дней до продления / ххх ₽ в неделю")
    public void shouldSeeSnippet() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().should(
                hasText("Продление: через 7 дней 1 999 \u20BD / 7 дней"));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активация автопродления объявления")
    public void shouldActivateAutoProlong() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableFalse", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock()
                .should(hasText("До снятия: 7 дней\nПродлить за 1 999 \u20BD на 7 дней"));

        mockRule.delete();
        mockRule.newMock()
                .with("desktop/SessionAuthUser")
                .with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .with("desktop-lk/UserOffersCarsProductAllSaleActivateProlongable")
                .post();

        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().buttonContains("Продлить").click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed()).should(hasText("Автопродление включено"));
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().should(
                hasText("Продление: через 7 дней 1 999 \u20BD / 7 дней"));
        basePageSteps.onLkSalesNewPage().getSale(0).autoProlongFailedBanner().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение баннера «Проверьте кошелёк»")
    public void shouldSeeCheckWalletBanner() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().checkWalletBanner().should(isDisplayed());
        basePageSteps.onLkSalesNewPage().checkWalletBanner().title().should(hasText("Проверьте кошелёк"));
        basePageSteps.onLkSalesNewPage().checkWalletBanner().text().should(hasText("Если в кошельке недостаточно денег для " +
                "продления, объявления будут сняты в конце их срока действия."));
        basePageSteps.onLkSalesNewPage().checkWalletBanner().link().should(hasText("Подробнее"));
        basePageSteps.onLkSalesNewPage().checkWalletBanner().closeButton().should(isDisplayed());
        basePageSteps.onLkSalesNewPage().checkWalletBanner().addFundsButton().should(hasText("Пополнить кошелёк"));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке закрытия баннера «Проверьте кошелёк»")
    public void shouldCloseWalletBanner() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().checkWalletBanner().should(isDisplayed());
        basePageSteps.onLkSalesNewPage().checkWalletBanner().closeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().checkWalletBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке Подробнее баннера «Проверьте кошелёк»")
    public void shouldClickMoreDetailsBanner() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().checkWalletBanner().link().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DEALER).path(PROFI).shouldNotSeeDiff();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Пополнить кошелёк»")
    public void shouldClickAddFunds() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().checkWalletBanner().addFundsButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MY).path(WALLET).shouldNotSeeDiff();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа при наведении на «количество дней до продления»")
    public void shouldSeePopup() {
        mockRule.with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().daysLeft().waitUntil(isDisplayed()).hover();
        basePageSteps.onLkSalesNewPage().activePopup().should(hasText("Размещение на 7 дней\n" +
                "Стоимость размещения стала ниже! Экономьте на быстрой продаже — каждую " +
                "неделю объявление будет автоматически обновляться и продлеваться за 1 999 ₽.\n" +
                "Подробнее"));
        basePageSteps.onLkSalesNewPage().activePopupLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DEALER).path(PROFI).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Активация неактивного объявления")
    public void shouldActivateSale() {
        mockRule.with(
                "desktop/UserWithTiedCard",
                "desktop-lk/UserOffersCarsActivateUserQuota",
                "desktop-lk/BillingAutoruPaymentInitActivateTiedCard",
                "desktop-lk/BillingAutoruPaymentProcessTiedCard",
                "desktop/BillingAutoruPayment");

        mockRule.with(mockRule.setOfferExpireDate("desktop-lk/UserOffersCarsIdActiveProlongableTrue", EXPIRE_DATE))
                .with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsInactiveProlongableTrue", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();

        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().buttonContains("Разместить").waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame()
                .waitUntil("Ожидаем загрузки фрейма поп-апа платежа", isDisplayed(), 10);
        basePageSteps.onLkSalesNewPage().switchToBillingFrame();
        basePageSteps.onLkSalesNewPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().billingPopup().successMessage().waitUntil(hasText("Платёж совершён успешно"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Платёж прошёл"));
        basePageSteps.onLkSalesNewPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().should(
                hasText("Продление: через 7 дней 1 999 \u20BD / 7 дней"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Активация неактивного объявления со скидкой")
    public void shouldActivateSaleWithDiscount() {
        mockRule.with(
                "desktop/UserWithTiedCard",
                "desktop-lk/BillingAutoruPaymentInitDiscountNewLK",
                "desktop-lk/BillingAutoruPaymentProcessTiedCard",
                "desktop/BillingAutoruPayment");
        mockRule.with(mockRule.setOfferExpireDate("desktop-lk/UserOffersCarsIdActiveProlongableTrue", EXPIRE_DATE))
                .with(mockRule.setUserOffersExpireDate("desktop-lk/UserOffersCarsInactiveProlongableTrueDiscount", EXPIRE_DATE))
                .post();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().waitUntil(
                hasText("Срок размещения закончился. Продлите размещение со скидкой 43%\nПродлить за 1 349 \u20BD на 7 дней"));
        basePageSteps.onLkSalesNewPage().getSale(0).statusBlock().buttonContains("Продлить за").click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame()
                .waitUntil("Ожидаем загрузки фрейма поп-апа платежа", isDisplayed(), 10);
        basePageSteps.onLkSalesNewPage().switchToBillingFrame();
        basePageSteps.onLkSalesNewPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().billingPopup().successMessage().waitUntil(hasText("Платёж совершён успешно"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Платёж прошёл"));
        basePageSteps.onLkSalesNewPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame().waitUntil(not(isDisplayed()));
    }

}
