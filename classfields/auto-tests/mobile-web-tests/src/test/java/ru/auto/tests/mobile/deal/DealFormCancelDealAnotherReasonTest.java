package ru.auto.tests.mobile.deal;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.junit.runners.Parameterized.UseParametersRunnerFactory;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.auto.tests.desktop.consts.Pages.DEALS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка. Форма. Отмена сделки")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealFormCancelDealAnotherReasonTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";
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

    @Parameter
    public String getDealMock;

    @Parameter(1)
    public String updateDealMock;

    @Parameters(name = "{0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {"desktop-lk/SafeDealDealGetWithOffer", "desktop/SafeDealDealUpdateSellerCancelFromDealPage2"},
                {"desktop-lk/SafeDealDealGetWithOfferForBuyer", "desktop/SafeDealDealUpdateBuyerCancelFromDealPage2"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                getDealMock,
                updateDealMock
                ).post();


        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку c причиной Другое")
    public void shouldCancelDealWithAnotherReason() {
        basePageSteps.onDealPage().button("Отменить сделку").click();
        basePageSteps.onDealPage().popup().waitUntil(isDisplayed());
        basePageSteps.onDealPage().popup().reasonItem("Другое").click();
        basePageSteps.onDealPage().popup().input().sendKeys("test test");
        basePageSteps.onDealPage().popup().button("Отправить").click();
        urlSteps.testing().path(MY).path(DEALS).shouldNotSeeDiff();
    }
}
