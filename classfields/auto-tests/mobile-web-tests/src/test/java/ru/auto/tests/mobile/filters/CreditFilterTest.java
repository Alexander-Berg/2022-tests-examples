package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Фильтры по кредиту")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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
                stub("mobile/SearchCarsAll"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkCreditProductCalculator"),
                stub("mobile/SearchCarsWithCreditFullFilter"),
                stub("mobile/SearchCarsCountWithCreditFullFilters"),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтры по кредиту должны примениться к поиску")
    public void shouldApplyCreditFilter() {
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();

        basePageSteps.onListingPage().paramsPopup().inactiveToggle("В кредит").click();
        basePageSteps.dragAndDropWithHover(basePageSteps.onListingPage().paramsPopup().paymentSliderFrom(), 50, 0);
        basePageSteps.dragAndDropWithHover(basePageSteps.onListingPage().paramsPopup().paymentSliderTo(), -50, 0);
        basePageSteps.dragAndDropWithHover(basePageSteps.onListingPage().paramsPopup().yearSliderTo(), -100, 0);
        basePageSteps.onListingPage().paramsPopup().input("Первый взнос", "100000");

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ON_CREDIT)
                .addParam("credit_payment_from", "7000")
                .addParam("price_from", "426000")
                .addParam("credit_payment_to", "49000")
                .addParam("price_to", "3085000")
                .addParam("credit_loan_term", "72")
                .addParam("credit_initial_fee", "100000")
                .shouldNotSeeDiff();

        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().should(hasText(
                containsString("Показать 27 925 предложений"))).click();

        basePageSteps.onListingPage().sortBar().offersCount().should(hasText("27 925 предложений"));
        basePageSteps.onListingPage().salesList().should(hasSize(1));
    }
}
