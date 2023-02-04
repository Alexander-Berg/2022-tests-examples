package ru.auto.tests.cabinet.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка. Задать вопрос")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AskQuestionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/DealerInfoMultipostingDisabled",
                "cabinet/UserOffersCarsUsed",
                "cabinet/CommonCustomerGet",
                "cabinet/CommonFeedbackMessageSend").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
        steps.onCabinetOffersPage().header().askQuestions().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Блок «Задайте вопрос»")
    public void shouldSeePersonalMenuBlock() {
        steps.onCabinetOffersPage().header().feedbackPopup()
                .should(hasText("Задайте вопрос\nТема\nТекст сообщения, поле обязательно\nОтправить\n" +
                        "Или позвоните нам:\n+7 495 755-55-77\n8 800 234-28-86"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Отправка сообщения")
    public void shouldSeeSuccessSendMessage() {
        sendMessage();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Закрытие отправки сообщения")
    public void shouldSeeClosedFeedbackPopup() {
        steps.onCabinetOffersPage().header().feedbackPopup().close().click();
        steps.onCabinetOffersPage().header().feedbackPopup().should(not(isDisplayed()));
    }

    @Step("Отправить сообщение")
    private void sendMessage() {
        steps.onCabinetOffersPage().header().feedbackPopup().subject().waitUntil(isDisplayed()).click();
        steps.onCabinetOffersPage().header().feedbackPopup().subject().sendKeys("Тема");

        steps.onCabinetOffersPage().header().feedbackPopup().message().click();
        steps.onCabinetOffersPage().header().feedbackPopup().message().sendKeys("Сообщение");

        steps.onCabinetOffersPage().header().feedbackPopup().send().click();
        steps.onAgencyCabinetMainPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Сообщение успешно отправлено"));
    }
}
