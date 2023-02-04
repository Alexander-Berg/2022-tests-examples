package ru.auto.tests.mobile.credits;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок кредитов на карточке под зарегом")
@Feature(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class InactiveCreditBlockOfferPageTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String LAND_ROVER = "land_rover";
    private static final String DISCOVERY = "discovery";

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
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/User"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditProductCalculator"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditApplicationActiveWithOffersEmpty"),
                stub("desktop/SharkCreditApplicationActiveWithPersonProfiles"),
                stub("desktop/SharkCreditApplicationCreate"),
                stub("desktop/SharkCreditApplicationUpdate")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по предложению кредита")
    public void shouldClickCreditOffer() {
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().creditOffer().click();

        assertThat("Не произошел скролл к блоку кредита", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Issue("AUTORUFRONT-20180")
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение визарда после короткой заявки")
    public void shouldFillCreditApplication() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardCreditBlock().button("Подтвердить"));
        basePageSteps.onCardPage().creditCurtain().waitUntil(isDisplayed()).should(hasText("Позже\nДалее\n" +
                "Паспортные данные\nСерия и номер паспорта\nДата выдачи паспорта\nКод подразделения\n" +
                "Кем выдан\nДата рождения\nМесто рождения"));

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(LAND_ROVER).path(DISCOVERY).path(SALE_ID)
                .shouldNotSeeDiff();
    }

}
