package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Кредит Ecredit» на карточке под зарегом")
@Feature(AutoruFeatures.CREDITS)
@Story(AutoruFeatures.DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditDealerEcreditAuthTest {

    private static final String SALE_ID = "1114956972-8c572ba1";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/OfferCarsUsedUserWithDealerEcredit"),
                stub("desktop/SharkBankList"),
                stub("desktop-lk/SharkCreditProductList"),
                stub("desktop-lk/SharkCreditApplicationActiveWithOffersEmpty"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по предложению от дилера")
    public void shouldClickDealersCreditOffer() {
        basePageSteps.onCardPage().cardHeader().creditOffer().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardCreditBlock().waitUntil(isDisplayed());

        assertThat("Не произошел скролл к блоку кредита", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Показываем заявку на кредит от дилера, если куплена эта услуга")
    public void shouldSeeDealersCredit() {
        basePageSteps.onCardPage().cardCreditBlock().hover();
        basePageSteps.onCardPage().cardCreditBlock().should(hasText("Подбор автокредита у дилера в разных банках" +
                "\nЗаполните заявку - получите решение онлайн\nСАЛОНА НЕ СУЩЕСТВУЕТ СОВСЕМ!!!\nСумма кредита\n" +
                "Срок кредита\n7 лет\nПервый взнос\n0 ₽\nПлатеж\n20 500 ₽ / мес.\nФИО\nЭлектронная почта\nНомер " +
                "телефона\nПодтвердить\nДаю согласие ООО «Яндекс.Вертикали», ООО «Кредитит» и «САЛОНА НЕ " +
                "СУЩЕСТВУЕТ СОВСЕМ!!!» на обработку данных в целях рассмотрения заявки и на условия обработки " +
                "данных. ООО «Кредитит» осуществляет содействие в подборе финансовых услуг."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отправляем заявку на кредит от дилера")
    public void shouldFillCreditRequest() {
        mockRule.setStubs(
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditApplicationCreate"),
                stub("desktop/SharkCreditProductListByCreditApplicationEcredit")
        ).update();

        basePageSteps.onCardPage().cardCreditBlock().hover();
        basePageSteps.onCardPage().cardCreditBlock().button("Подтвердить").waitUntil(isDisplayed()).click();

        urlSteps.testing().path(PROMO).path(FINANCE).shouldNotSeeDiff();
    }
}
