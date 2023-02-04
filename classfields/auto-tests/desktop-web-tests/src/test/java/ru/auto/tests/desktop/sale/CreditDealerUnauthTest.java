package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_MAX_PAGE;
import static ru.auto.tests.desktop.element.card.CardCreditBlock.CREDIT_CARD_TEXT_UNAUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Кредит от дилера» на карточке под незарегом")
@Feature(AutoruFeatures.CREDITS)
@Story(AutoruFeatures.DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Category({Regression.class, Testing.class})
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreditDealerUnauthTest {

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
                stub("desktop/SharkBankList"),
                stub("desktop-lk/SharkCreditProductList"),
                stub(saleMock)
        ).create();

        basePageSteps.setWindowSize(WIDTH_MAX_PAGE, HEIGHT_1024);

        urlSteps.testing().path(CARS).path(section).path(SALE).path(SALE_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Показываем заявку на кредит от дилера, если куплена эта услуга")
    public void shouldSeeDealersCredit() {
        basePageSteps.onCardPage().cardCreditBlock().should(hasText(CREDIT_CARD_TEXT_UNAUTH));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по предложению от дилера")
    public void shouldClickDealersCreditOffer() {
        basePageSteps.onCardPage().cardHeader().creditOffer().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardCreditBlock().waitUntil(isDisplayed());
        assertThat("Не произошел скролл к блоку кредита", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по предложению от дилера в попапе цены")
    public void shouldClickDealersCreditOfferInPricePopup() {
        basePageSteps.onCardPage().cardHeader().price().should(isDisplayed()).click();
        basePageSteps.onCardPage().pricePopup().creditOffer().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardCreditBlock().waitUntil(isDisplayed());
        assertThat("Не произошел скролл к блоку кредита", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Заявка на кредит")
    public void shouldFillCreditRequest() {
        mockRule.setStubs(
                stub("desktop/UserPhones"),
                stub("desktop/AuthLoginOrRegister"),
                stub("desktop/UserConfirm"),
                stub("desktop/ProductsUsedUnauth"),
                stub("desktop/ProductsNewUnauth")
        ).update();

        basePageSteps.onCardPage().cardCreditBlock().input("ФИО", "Иван Иванов");
        basePageSteps.onCardPage().cardCreditBlock().input("Телефон", "+79111111111");
        basePageSteps.onCardPage().cardCreditBlock().button("Отправить заявку").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().cardCreditBlock().input("Код из смс").waitUntil(isDisplayed())
                .sendKeys("1234");
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCardPage().cardCreditBlock().button("Отправить заявку").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Ваша заявка отправлена дилеру"));
    }
}
