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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.element.lk.CreditFilter.FILTER_ALL;
import static ru.auto.tests.desktop.element.lk.CreditFilter.FILTER_CASH;
import static ru.auto.tests.desktop.element.lk.CreditFilter.FILTER_REFIN;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кредитный брокер")
@Feature(AutoruFeatures.CREDITS)
@Story(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditsFilterTest {

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
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductListWithoutParams"),
                stub("desktop/SharkCreditApplicationListWithOffers"),
                stub("desktop-lk/SharkCreditProductListByCreditApplication2"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive")
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр про одному продукту")
    public void shouldFilterClaimsList() {
        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().button(FILTER_CASH).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(4));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_CASH).waitUntil(isDisplayed());

        basePageSteps.onLkCreditsPage().filter().button(FILTER_ALL).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_ALL).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр про нескольким продуктам")
    public void shouldMultifilterClaimsList() {
        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().button(FILTER_CASH).waitUntil(isDisplayed()).click();
        basePageSteps.onLkCreditsPage().filter().button(FILTER_REFIN).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(5));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_CASH).waitUntil(isDisplayed());
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_REFIN).waitUntil(isDisplayed());

        basePageSteps.onLkCreditsPage().filter().button(FILTER_ALL).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_ALL).should(isDisplayed());
    }
}