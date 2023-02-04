package ru.auto.tests.desktop.lk.credits;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.DRAFT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockSharkCreditApplication.creditApplicationActive;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.creditProducts;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestAllBody;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestGeoBaseIdsBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.SHARK_CREDIT_APPLICATION_ACTIVE;
import static ru.auto.tests.desktop.mock.Paths.SHARK_CREDIT_PRODUCT_LIST;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;

@DisplayName("Редирект на форму редактирования")
@Feature(AutoruFeatures.CREDITS)
@Story("Редирект")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditEditRedirectTest {

    private static final String TRUE = "true";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/SharkBankList"),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestGeoBaseIdsBody())
                        .withResponseBody(creditProducts().getResponse()),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestAllBody())
                        .withResponseBody(creditProducts().getResponse()),
                stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                        .withRequestQuery(query().setWithOffers(TRUE))
                        .withResponseBody(creditApplicationActive().getResponse()),
                stub().withGetDeepEquals(SHARK_CREDIT_APPLICATION_ACTIVE)
                        .withRequestQuery(query().setWithOffers(TRUE).setWithPersonProfiles(TRUE))
                        .withResponseBody(creditApplicationActive().getResponse())
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должен быть редирект на страницу редактирования")
    public void shouldRedirectToEditPage() {
        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }
}
