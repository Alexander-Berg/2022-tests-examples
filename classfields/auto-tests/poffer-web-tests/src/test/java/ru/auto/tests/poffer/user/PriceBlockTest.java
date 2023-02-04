package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.models.Offer;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - блок цены")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PriceBlockTest {

    private static final String PRICE_ERROR = "Укажите цену от 1 500 до 1 000 000 000 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Offer offer;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/Currencies"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Конвертация валют")
    public void shouldConvertPrice() {
        pofferSteps.selectCurrency("₽", "$");
        pofferSteps.onBetaPofferPage().priceBlock().priceInput().waitUntil(hasValue("7 731"));
        pofferSteps.selectCurrency("$", "€");
        pofferSteps.onBetaPofferPage().priceBlock().priceInput().waitUntil(hasValue("6 478"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена меньше минимальной")
    public void shouldSeeMinPriceError() {
        offer.setPrice("1000");
        pofferSteps.enterPrice();
        pofferSteps.selectCurrency("₽", "₽");

        pofferSteps.onBetaPofferPage().priceBlock().priceError().waitUntil(hasText(PRICE_ERROR));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена больше максимальной")
    public void shouldSeeMaxPriceError() {
        offer.setPrice("1000000001");
        pofferSteps.enterPrice();
        pofferSteps.selectCurrency("₽", "₽");

        pofferSteps.onBetaPofferPage().priceBlock().priceError().waitUntil(hasText(PRICE_ERROR));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Пояснение про обмен")
    public void shouldSeeExchangePopup() {
        pofferSteps.onBetaPofferPage().priceBlock().exchangeInfoIcon().hover();

        pofferSteps.onBetaPofferPage().popup().waitUntil(isDisplayed())
                .should(hasText("В объявлении появится пометка, что вы готовы обменять авто. " +
                        "Можете уточнить в описании, какие модели вам интересны.\n⚠️ Нельзя предлагать " +
                        "исключительно обмен — вы должны быть готовы продать машину. " +
                        "Иначе мы заблокируем объявление."));
    }
}