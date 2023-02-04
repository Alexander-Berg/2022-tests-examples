package ru.auto.tests.desktop.deal;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.auto.tests.desktop.consts.Pages.DEALS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.junit.runners.Parameterized.UseParametersRunnerFactory;

@DisplayName("Безопасная сделка. Форма. Отмена сделки")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealFormCancelDealTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";
    private final static String BUYER_REASON = "Договорился с другим продавцом";
    private final static String SELLER_REASON = "Договорился с другим покупателем";

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

    @Parameter(2)
    public String reasonText;

    @Parameters(name = "{0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {"desktop-lk/SafeDealDealGetWithOffer", "desktop/SafeDealDealUpdateSellerCancelFromDealPage", SELLER_REASON},
                {"desktop-lk/SafeDealDealGetWithOfferForBuyer", "desktop/SafeDealDealUpdateBuyerCancelFromDealPage", BUYER_REASON}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                getDealMock,
                updateDealMock).post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку")
    public void shouldCancelDeal() {
        basePageSteps.onDealPage().button("Отменить сделку").click();
        basePageSteps.onDealPage().popup().waitUntil(isDisplayed());
        basePageSteps.onDealPage().popup().selectItem("Выберите причину", reasonText);
        basePageSteps.onDealPage().popup().button("Отправить").click();
        urlSteps.testing().path(MY).path(DEALS).shouldNotSeeDiff();
    }
}