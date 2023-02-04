package ru.auto.tests.desktop.credits;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.DRAFT;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WIZARD;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннер кредита в листинге")
@Feature(AutoruFeatures.CREDITS)
@Story(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InactiveCreditBannerListingTest {

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
                stub("desktop/User"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAll"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkCreditApplicationActiveEmpty"),
                stub("desktop/SharkCreditApplicationActiveWithOffersEmptyAndDraft"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfiles"),
                stub("desktop/SharkCreditApplicationCreateThreeMillions"),
                stub("desktop/SharkCreditApplicationUpdate"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должен открыться попап короткой заявки и ЛК с визардом")
    public void shouldOpenPopupAndWizard() {
        basePageSteps.onListingPage().creditBanner().should(isDisplayed()).click();
        basePageSteps.onListingPage().creditApplicationPopup().should(isDisplayed());
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }
}