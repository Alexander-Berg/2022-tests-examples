package ru.auto.tests.mobile.credits;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.DRAFT;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockSharkCreditApplication.creditApplicationActive;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.creditProducts;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestAllBody;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestGeoBaseIdsBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.SHARK_CREDIT_APPLICATION_ACTIVE;
import static ru.auto.tests.desktop.mock.Paths.SHARK_CREDIT_PRODUCT_LIST;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Ссылка кредита в сниппете листинга под зарегом")
@Feature(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class CreditLinkListingSnippetRegTest {

    private final static String CREATE_CREDIT_REQUEST = "credits/create_credit_request.json";
    private static final String TRUE = "true";

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
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("mobile/SearchCarsOneUserSaleForCredit"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditProductCalculator"),
                stub("desktop/SharkCreditApplicationCreate"),
                stub("desktop/SharkCreditApplicationUpdate"),
                stub("desktop/SharkBankList"),
                stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                        .withRequestQuery(query().setWithOffers(TRUE)),
                stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                        .withRequestQuery(query().setWithOffers(TRUE).setWithPersonProfiles(TRUE))
                        .withResponseBody(creditApplicationActive().getResponse()),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestGeoBaseIdsBody())
                        .withResponseBody(creditProducts().getResponse()),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestAllBody())
                        .withResponseBody(creditProducts().getResponse())
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение драфта после короткой заявки")
    public void shouldFillCreditApplication() {
        basePageSteps.onListingPage().getSale(0).creditPrice().click();
        basePageSteps.onListingPage().creditApplicationPopup().should(isDisplayed());
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        mockRule.overwriteStub(9, stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                .withRequestQuery(query().setWithOffers(TRUE))
                .withResponseBody(creditApplicationActive().getResponse()));

        basePageSteps.onLkCreditsDraftPage().carInfo().waitUntil(isDisplayed()).should(hasText(
                "Кредит почти ваш\n" +
                        "500 000 ₽\n" +
                        "5 лет"));
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

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/mobile/createCreditApplication/",
                hasJsonBody(CREATE_CREDIT_REQUEST)
        ));
    }

}
