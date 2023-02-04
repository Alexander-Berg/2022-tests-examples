package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.COMPLAIN_SUCCESSFULLY_SENDED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CALLTRACKING_CALL_COMPLAINT;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@Story("Звонки в б/у")
@DisplayName("Кабинет дилера. Звонки в б/у")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsUsedTest {

    private static final String COMPLAIN_REASON = "Звонок по новому авто";
    private static final String EMAIL = "test@auto.ru";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/Session/DirectDealerMoscow"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerTariff/AllTariffs"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CalltrackingUsed"),
                stub("cabinet/CalltrackingAggregated")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Пожаловаться» у звонка с базовой ценой")
    public void shouldNotSeeComplainButtonOnBasePriceCall() {
        basePageSteps.onCallsPage().getCall(0).hover();
        basePageSteps.onCallsPage().getCall(0).menuButton().click();

        basePageSteps.onCallsPage().menu().menuItem("Пожаловаться").should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются цены звонков")
    public void shouldSeeCallsPrices() {
        basePageSteps.onCallsPage().getCall(0).price().should(hasText(formatPrice(1)));
        basePageSteps.onCallsPage().getCall(1).price().should(hasText(formatPrice(600)));
        basePageSteps.onCallsPage().getCall(2).price().should(hasText("—"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жалоба по причине «Звонок по новому авто»")
    public void shouldClickComplainButton() {
        basePageSteps.onCallsPage().getCall(1).hover();
        basePageSteps.onCallsPage().getCall(1).menuButton().click();
        basePageSteps.onCallsPage().menu().menuItem("Пожаловаться").waitUntil(isDisplayed()).click();
        basePageSteps.onCallsPage().complaintPopup().selectItem("Причина жалобы", COMPLAIN_REASON);
        basePageSteps.onCallsPage().complaintPopup().input("Электронная почта", EMAIL);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("call_id", "1199385");
        requestBody.addProperty("text", COMPLAIN_REASON);
        requestBody.addProperty("email", EMAIL);

        mockRule.setStubs(
                stub().withPostDeepEquals(CALLTRACKING_CALL_COMPLAINT)
                        .withRequestBody(requestBody)
                        .withStatusSuccessResponse()
        ).update();

        basePageSteps.onCallsPage().complaintPopup().button("Отправить").click();
        basePageSteps.onCallsPage().notifier().waitUntil(isDisplayed())
                .should(hasText(COMPLAIN_SUCCESSFULLY_SENDED));
    }

}
