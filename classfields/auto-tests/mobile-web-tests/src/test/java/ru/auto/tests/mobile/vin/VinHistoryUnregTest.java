package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Urls.FORMS_YANDEX_PARTNERSHIP;
import static ru.auto.tests.desktop.mobile.page.HistoryPage.QUESTION_BUTTON_TEXT;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Про авто - блок «Проверка по VIN» под незарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VinHistoryUnregTest {

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
        mockRule.newMock().with("desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кноке «?»")
    public void shouldClickQuestionButton() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().questionButton().click();
        basePageSteps.onHistoryPage().popup().waitUntil(hasText(QUESTION_BUTTON_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кноке «Войти»")
    public void shouldClickEnterButton() {
        String returnUrl = urlSteps.getCurrentUrl();
        basePageSteps.onHistoryPage().button("Войти").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s?action=scroll-to-reports", returnUrl))).shouldNotSeeDiff();

        authorize();
        urlSteps.fromUri(returnUrl).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Сотрудничество»")
    public void shouldClickCollaborationUrl() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.onHistoryPage().button("Сотрудничество").click();
        urlSteps.fromUri(FORMS_YANDEX_PARTNERSHIP)
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта по VIN")
    public void shouldBuySingleReportByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "4S2CK58D924333406");
        basePageSteps.onHistoryPage().findButton().click();
        String returnUrl = urlSteps.getCurrentUrl();
        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 499\u00a0₽").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s?from=api_m_vincheck&vinHistoryButton=CardVinReportSingleButton",
                        returnUrl)))
                .addParam("from", "api_m_vincheck").shouldNotSeeDiff();

        authorize();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        urlSteps.fromUri(returnUrl).addParam("from", "api_m_vincheck").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта по госномеру")
    public void shouldBuySingleReportByLicensePlate() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "Y151BB178");
        basePageSteps.onHistoryPage().findButton().click();
        String returnUrl = urlSteps.getCurrentUrl();
        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 499\u00a0₽").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s?from=api_m_vincheck&vinHistoryButton=CardVinReportSingleButton",
                        returnUrl)))
                .addParam("from", "api_m_vincheck").shouldNotSeeDiff();

        authorize();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        urlSteps.fromUri(returnUrl).addParam("from", "api_m_vincheck").shouldNotSeeDiff();
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.newMock().with("desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid").post();

        basePageSteps.onAuthPage().input("Номер телефона").sendKeys("9111111111");
        basePageSteps.onAuthPage().input("Код из смс", "1234");
    }
}
