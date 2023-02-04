package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.models.MobileOffer;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Конвертация валют")
@Epic(BETA_POFFER)
@Feature("Конвертация валют")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class PriceBlockTest {

    private static final String PRICE_ERROR = "Укажите цену от 1 500 до 1 000 000 000 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MobileOffer offer;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/Currencies"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody())
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Конвертация валют")
    public void shouldConvertPrice() {
        pofferSteps.selectCurrency("₽", "$");
        pofferSteps.onPofferPage().priceBlock().priceInput().waitUntil(hasValue("7 731"));
        pofferSteps.selectCurrency("$", "€");
        pofferSteps.onPofferPage().priceBlock().priceInput().waitUntil(hasValue("6 478"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена меньше минимальной")
    public void shouldSeeMinPriceError() {
        offer.setPrice("1000");
        pofferSteps.enterPrice();
        pofferSteps.selectCurrency("₽", "₽");

        pofferSteps.onPofferPage().priceBlock().priceError().waitUntil(hasText(PRICE_ERROR));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена больше максимальной")
    public void shouldSeeMaxPriceError() {
        offer.setPrice("1000000001");
        pofferSteps.enterPrice();
        pofferSteps.selectCurrency("₽", "₽");

        pofferSteps.onPofferPage().priceBlock().priceError().waitUntil(hasText(PRICE_ERROR));
    }

}
