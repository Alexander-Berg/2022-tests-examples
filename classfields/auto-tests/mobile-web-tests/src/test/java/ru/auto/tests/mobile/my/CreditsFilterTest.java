package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mobile.element.lk.CreditFilter.FILTER_ALL;
import static ru.auto.tests.desktop.mobile.element.lk.CreditFilter.FILTER_CASH;
import static ru.auto.tests.desktop.mobile.element.lk.CreditFilter.FILTER_REFIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кредитный брокер")
@Feature(AutoruFeatures.LK)
@Story(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CreditsFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SharkBankList",
                "desktop/SharkCreditProductListWithoutParams",
                "desktop/SharkCreditApplicationListWithOffers",
                "desktop-lk/SharkCreditProductListByCreditApplication2",
                "desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive").post();

        urlSteps.testing().path(MY).path(CREDITS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр про продукту")
    public void shouldFilterClaimsList() {
        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().button(FILTER_CASH).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(4));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_CASH).waitUntil(isDisplayed());

        basePageSteps.onLkCreditsPage().filter().button(FILTER_ALL).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_ALL).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр про нескольким продуктам")
    public void shouldMultifilterClaimsList() {
        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().button(FILTER_CASH).waitUntil(isDisplayed()).click();
        basePageSteps.onLkCreditsPage().filter().button(FILTER_REFIN).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().waitUntil(hasSize(5));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_CASH).waitUntil(isDisplayed());

        basePageSteps.onLkCreditsPage().filter().button(FILTER_ALL).waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().filter().buttonChecked(FILTER_ALL).waitUntil(isDisplayed());
    }
}
