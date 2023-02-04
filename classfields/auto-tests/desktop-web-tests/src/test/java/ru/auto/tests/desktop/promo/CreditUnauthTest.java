package ru.auto.tests.desktop.promo;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CREDITS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.creditProducts;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestAllBody;
import static ru.auto.tests.desktop.mock.MockSharkCreditProducts.requestGeoBaseIdsBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.SHARK_CREDIT_PRODUCT_LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - Кредитный брокер")
@Feature(AutoruFeatures.PROMO)
@Story(CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditUnauthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SharkBankList"),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestGeoBaseIdsBody())
                        .withResponseBody(creditProducts().getResponse()),
                stub().withPostDeepEquals(SHARK_CREDIT_PRODUCT_LIST)
                        .withRequestBody(requestAllBody())
                        .withResponseBody(creditProducts().getResponse())
        ).create();

        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().header().line2().button(CREDITS).click();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение заголовка страницы для незарега")
    public void shouldSeePromoHeader() {
        urlSteps.testing().path(PROMO).path(FINANCE).shouldNotSeeDiff();
        basePageSteps.onPromoCreditPage().creditHeader().should(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение формы заявки на кредит для незарега")
    public void shouldSeeCreditForm() {
        urlSteps.testing().path(PROMO).path(FINANCE).shouldNotSeeDiff();
        basePageSteps.onPromoCreditPage().form().should(isDisplayed());
    }
}