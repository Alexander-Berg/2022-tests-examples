package ru.auto.tests.cabinet.agency.header;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(AGENCY_CABINET)
@Story(HEADER)
@DisplayName("Кабинет агента. Шапка. Задать вопрос")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AskQuestionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/SessionAgency",
                "cabinet/DealerAccountAgency",
                "cabinet/CommonCustomerGetAgency",
                "cabinet/AgencyAgencyGet",
                "cabinet/AgencyBillingRenewalAutorenewalGet",
                "cabinet/AgencyClientsPresetsGet",
                "cabinet/ApiWorkspaceAutoruTagAgentProductActivationsSumFind",
                "cabinet/DealerWalletProductActivationsTotalStats",
                "cabinet/CommonFeedbackMessageSend").post();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
        steps.waitUntilPageIsFullyLoaded();
        steps.onCabinetOffersPage().header().askQuestions().click();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок «Задайте вопрос»")
    public void shouldSeePersonalMenuBlock() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOffersPage().header().feedbackPopupWithoutBackground());

        urlSteps.setProduction().open();
        steps.waitUntilPageIsFullyLoaded();
        steps.onCabinetOffersPage().header().askQuestions().click();

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOffersPage().header().feedbackPopupWithoutBackground());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Отправка сообщения")
    public void shouldSeeSuccessSendMessage() {
        steps.onAgencyCabinetMainPage().header().feedbackPopup().subject().click();
        steps.onAgencyCabinetMainPage().header().feedbackPopup().subject().sendKeys("Тема");

        steps.onAgencyCabinetMainPage().header().feedbackPopup().message().click();
        steps.onAgencyCabinetMainPage().header().feedbackPopup().message().sendKeys("Сообщение");

        steps.onAgencyCabinetMainPage().header().feedbackPopup().send().click();
        steps.onAgencyCabinetMainPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Сообщение успешно отправлено"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Закрытие отправки сообщения")
    public void shouldSeeClosedFeedbackPopup() {
        steps.onAgencyCabinetMainPage().header().feedbackPopup().close().click();
        steps.onAgencyCabinetMainPage().header().feedbackPopup().should(not(isDisplayed()));
    }
}
