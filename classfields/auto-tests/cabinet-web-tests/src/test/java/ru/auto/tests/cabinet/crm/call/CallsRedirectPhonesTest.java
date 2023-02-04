package ru.auto.tests.cabinet.crm.call;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.NUMBERS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки. Подменные номера")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsRedirectPhonesTest {

    public static final String SALON_PHONE = "+74951350605";
    public static final String REDIRECT_PHONE = "+74994270055";
    public static final String CHECK_PHONE = "+79111111111";

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/CrmClientsGet",
                "cabinet/DealerAccount",
                "cabinet/DealerPhonesRedirect",
                "cabinet/DealerPhonesRedirectOriginalPhone",
                "cabinet/DealerPhonesRedirectRedirectPhone",
                "cabinet/ApiAutoDealersRedirect",
                "cabinet/ApiAutoDealersComplaintRedirect").post();

        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).path(NUMBERS).addParam(CLIENT_ID, "16453").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение страницы")
    public void shouldSeePage() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsRedirectPhonesPage().content());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsRedirectPhonesPage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Фильтр по номеру салона")
    public void shouldFilterBySalonNumber() {
        basePageSteps.onCallsRedirectPhonesPage().input("Номер салона", SALON_PHONE);
        basePageSteps.onCallsRedirectPhonesPage().phoneNumbersList().waitUntil(hasSize(1));
        basePageSteps.onCallsRedirectPhonesPage().getPhoneNumber(0)
                .should(hasText(format("%s\n%s\nПожаловаться", SALON_PHONE, REDIRECT_PHONE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Фильтр по подменному номеру")
    public void shouldFilterByRedirectPhone() {
        basePageSteps.onCallsRedirectPhonesPage().input("Подменный номер", REDIRECT_PHONE);
        basePageSteps.onCallsRedirectPhonesPage().phoneNumbersList().waitUntil(hasSize(1));
        basePageSteps.onCallsRedirectPhonesPage().getPhoneNumber(0)
                .should(hasText(format("%s\n%s\nПожаловаться", SALON_PHONE, REDIRECT_PHONE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пожаловаться»")
    public void shouldClickComplainButton() {
        basePageSteps.onCallsRedirectPhonesPage().getPhoneNumber(0).button("Пожаловаться").click();
        basePageSteps.onCallsPage().complaintPopup().selectItem("Причина жалобы",
                "Другая причина");
        basePageSteps.onCallsPage().complaintPopup()
                .input("Номер телефона, с которого производилась проверка", CHECK_PHONE);
        basePageSteps.onCallsPage().complaintPopup()
                .input("Описание жалобы", "Test");
        basePageSteps.onCallsPage().complaintPopup().button("Отправить").click();
        basePageSteps.onCallsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Жалоба успешно добавлена"));
    }
}
