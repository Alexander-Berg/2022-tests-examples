package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - фильтры по кредиту")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SearchCarsAll"),
                stub("desktop/SearchCarsCountWithCredit"),
                stub("desktop/SearchCarsWithCredit"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение поп-апа с фильтрами кредита")
    public void shouldSeeAutoruExclusivePopup() {
        basePageSteps.onListingPage().filter().checkbox("В кредит").click();
        basePageSteps.onListingPage().creditFilterPopup().waitUntil(isDisplayed()).should(hasText("Узнайте ваш лимит " +
                "по кредиту\nПлатёж\n1 000 ₽ / мес.\n100 000 ₽ / мес.\n7 лет\nПервый взнос"));

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ON_CREDIT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтры по кредиту должны примениться к поиску")
    public void shouldApplyCreditFilter() {
        basePageSteps.onListingPage().filter().checkbox("В кредит").click();
        basePageSteps.dragAndDrop(basePageSteps.onListingPage().creditFilterPopup().paymentSliderFrom(), 50, 0);
        basePageSteps.dragAndDrop(basePageSteps.onListingPage().creditFilterPopup().paymentSliderTo(), -50, 0);
        basePageSteps.dragAndDrop(basePageSteps.onListingPage().creditFilterPopup().yearSliderTo(), -50, 0);
        basePageSteps.onListingPage().creditFilterPopup().input("Первый взнос", "100000");

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ON_CREDIT)
                .addParam("credit_payment_from", "10000")
                .addParam("price_from", "609000")
                .addParam("credit_payment_to", "46000")
                .addParam("price_to", "2903000")
                .addParam("credit_loan_term", "72")
                .addParam("credit_initial_fee", "100000")
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().resultsButton().waitUntil(hasText("Показать 17 587 предложений"));
        basePageSteps.onListingPage().filter().resultsButton().click();

        basePageSteps.onListingPage().salesList().should(hasSize(1));
    }
}