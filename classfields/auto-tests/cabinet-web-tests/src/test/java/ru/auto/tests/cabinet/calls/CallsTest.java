package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Кабинет дилера. Звонки")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingCallRecord",
                "cabinet/CalltrackingCallComplaint").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Скачать аудио»")
    public void shouldClickDownloadAudioButton() {
        basePageSteps.onCallsPage().getCall(0).hover();
        basePageSteps.onCallsPage().getCall(0).menuButton().click();
        basePageSteps.onCallsPage().menu().menuItem("Скачать аудио").waitUntil(isDisplayed()).click();
        basePageSteps.onCallsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Запись звонка успешно загружена"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пожаловаться»")
    public void shouldClickComplainButton() {
        basePageSteps.onCallsPage().getCall(0).hover();
        basePageSteps.onCallsPage().getCall(0).menuButton().click();
        basePageSteps.onCallsPage().menu().menuItem("Пожаловаться").waitUntil(isDisplayed()).click();
        basePageSteps.onCallsPage().complaintPopup().selectItem("Причина жалобы",
                "Звонок по авто с пробегом");
        basePageSteps.onCallsPage().complaintPopup().input("Электронная почта", "test@auto.ru");
        basePageSteps.onCallsPage().complaintPopup().button("Отправить").click();
        basePageSteps.onCallsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Жалоба успешно отправлена"));
    }
}
