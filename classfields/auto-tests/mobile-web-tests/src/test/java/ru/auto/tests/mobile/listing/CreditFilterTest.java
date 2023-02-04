package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг объявлений - фильтры по кредиту")
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
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with(
                "desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAll",
                "desktop/SharkBankList",
                "desktop/SharkCreditProductList",
                "mobile/SearchCarsCountWithCredit",
                "mobile/SearchCarsWithCredit").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Issue("AUTORUFRONT-21839")
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Кликаем в чекбокс фильтра по кредиту")
    public void shouldClickCreditFilterCheckbox() {
        basePageSteps.onListingPage().filters().checkbox("В кредит").click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ON_CREDIT).shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().offersCount().should(hasText("27 925 предложений"));
        basePageSteps.onListingPage().salesList().should(hasSize(1));
    }
}
