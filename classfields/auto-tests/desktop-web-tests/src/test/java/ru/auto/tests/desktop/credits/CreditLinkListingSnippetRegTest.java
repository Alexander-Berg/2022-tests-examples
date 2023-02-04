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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
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
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Ссылка кредита в сниппете листинга под зарегом")
@Feature(AutoruFeatures.CREDITS)
@Story(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class CreditLinkListingSnippetRegTest {

    private final static String CREATE_CREDIT_REQUEST = "credits/create_credit_request.json";

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

    @Inject
    private SeleniumMockSteps browserMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsOneUserSaleForCredit"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkCreditApplicationActiveWithOffersEmptyAndDraft"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfiles"),
                stub("desktop/SharkCreditApplicationCreate"),
                stub("desktop/SharkCreditApplicationUpdate"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение визарда после короткой заявки")
    public void shouldFillCreditApplication() {
        basePageSteps.onListingPage().getSale(0).creditPrice().click();
        basePageSteps.onListingPage().creditApplicationPopup().should(isDisplayed());
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должен привязаться сразу оффер к заявке")
    public void shouldSeeOfferInRequest() {
        basePageSteps.onListingPage().getSale(0).creditPrice().click();
        basePageSteps.onListingPage().creditApplicationPopup().should(isDisplayed());
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        browserMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/desktop/createCreditApplication/",
                hasJsonBody(CREATE_CREDIT_REQUEST)
        ));
    }
}
