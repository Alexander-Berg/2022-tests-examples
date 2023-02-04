package ru.auto.tests.mobile.credits;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.consts.Notifications.REQUEST_HAS_BEEN_SENT_TO_DEALER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.cardpage.CardCreditBlock.CREDIT_CARD_TEXT_AUTH;
import static ru.auto.tests.desktop.mobile.element.gallery.CreditPopup.SEND_REQUEST;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Кредит от дилера» на карточке под зарегом")
@Feature(AutoruFeatures.CREDITS)
@Story(AutoruFeatures.DEALERS)
@RunWith(Parameterized.class)
@GuiceModules({MobileEmulationTestsModule.class})
@Category({Regression.class, Testing.class})
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealersCreditSaleAuthTest {

    private static final String SALE_ID = "1076842087-f1e84";

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

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {USED, "desktop/OfferCarsUsedUserWithDealerCredit"},
                {NEW, "desktop/OfferCarsNewUserWithDealerCredit"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub(saleMock),
                stub("desktop/SessionAuthUser"),
                stub("mobile/SharkCreditProductList"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditApplicationActiveWithOffersEmpty")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по предложению от дилера")
    public void shouldClickDealersCreditOffer() {
        urlSteps.testing().path(CARS).path(section).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().creditOffer().click();

        assertThat("Не произошел скролл к блоку кредита", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение заявки на кредит при наличии этой услуги у дилера")
    public void shouldSeeDealersCredit() {
        urlSteps.testing().path(CARS).path(section).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().cardCreditBlock().should(hasText(CREDIT_CARD_TEXT_AUTH));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отправка заявки на кредит")
    public void shouldFillCreditRequest() {
        mockRule.setStubs(
                stub("desktop/CreditsDealerPreliminary"),
                stub("desktop/ProductsUsedAuth"),
                stub("desktop/ProductsNewAuth")
        ).update();

        urlSteps.testing().path(CARS).path(section).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().cardCreditBlock().should(hasText(CREDIT_CARD_TEXT_AUTH));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardCreditBlock().button(SEND_REQUEST));

        basePageSteps.onCardPage().notifier(REQUEST_HAS_BEEN_SENT_TO_DEALER).should(isDisplayed());
    }
}
