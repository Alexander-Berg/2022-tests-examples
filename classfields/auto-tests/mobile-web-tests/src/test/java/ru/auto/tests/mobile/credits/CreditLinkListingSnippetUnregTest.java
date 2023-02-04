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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.DRAFT;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
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

@DisplayName("Ссылка кредита в сниппете листинга под незарегом")
@Feature(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CreditLinkListingSnippetUnregTest {

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/AuthLoginOrRegister"),
                stub("desktop/UserConfirm"),
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
    @DisplayName("Отображение драфта после регистрации в короткой заявке")
    public void shouldFillCreditApplication() {
        basePageSteps.onListingPage().getSale(0).creditPrice().click();
        basePageSteps.onListingPage().creditApplicationPopup().should(isDisplayed());
        basePageSteps.onListingPage().creditApplicationPopup().input("ФИО", "Иван Иванов");
        basePageSteps.onListingPage().creditApplicationPopup().input("Электронная почта", "sosediuser1@mail.ru");
        basePageSteps.onListingPage().creditApplicationPopup().input("Номер телефона", "9111111111");
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        mockRule.overwriteStub(9, stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                .withRequestQuery(query().setWithOffers(TRUE))
                .withResponseBody(creditApplicationActive().getResponse()));
        mockRule.setStubs(
                stub("desktop/User"),
                stub("desktop/SessionAuthUser")
        ).update();

        basePageSteps.onListingPage().creditApplicationPopup().input("Код из SMS", "1234");

        basePageSteps.onLkCreditsDraftPage().carInfo().waitUntil(isDisplayed()).should(hasText(
                "Кредит почти ваш\n" +
                        "500 000 ₽\n" +
                        "5 лет"));
        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }

}
