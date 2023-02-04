package ru.auto.tests.amp.history;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Urls.FORMS_YANDEX_PARTNERSHIP;
import static ru.auto.tests.desktop.mobile.page.HistoryPage.QUESTION_BUTTON_TEXT;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Amp - Про авто")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class VinHistoryUnregTest {

    private static final String VIN = "4S2CK58D924333406";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices").post();

        urlSteps.testing().path(AMP).path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «?»")
    public void shouldClickQuestionButton() {
        basePageSteps.onHistoryPage().questionButton().click();
        basePageSteps.onHistoryPage().popup().waitUntil(hasText(QUESTION_BUTTON_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Войти»")
    public void shouldClickEnterButton() {
        basePageSteps.onHistoryPage().vinLoginButton().click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/history/", urlSteps.getConfig().getMobileURI())))
                .shouldNotSeeDiff();

        authorize();
        urlSteps.mobileURI().path(HISTORY).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по ссылке «Сотрудничество»")
    public void shouldClickCollaborationUrl() {
        basePageSteps.onHistoryPage().button("Сотрудничество").click();
        urlSteps.fromUri(FORMS_YANDEX_PARTNERSHIP).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Поиск отчёта по VIN")
    public void shouldFindReportByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        urlSteps.mobileURI().path(HISTORY).path(VIN).path(SLASH).ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Покупка пакета из 10 отчётов")
    public void shouldBuyPack10Report() {
        basePageSteps.onHistoryPage().vinPackagePromo().button("990\u00a0₽").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/history/", urlSteps.getConfig().getMobileURI())))
                .shouldNotSeeDiff();

        authorize();
        urlSteps.mobileURI().path(HISTORY).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Покупка пакета из 5 отчётов")
    public void shouldBuyPack5Report() {
        basePageSteps.onHistoryPage().vinPackagePromo().button("599\u00a0₽").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/history/", urlSteps.getConfig().getMobileURI())))
                .shouldNotSeeDiff();

        authorize();
        urlSteps.mobileURI().path(HISTORY).shouldNotSeeDiff();
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.newMock().with("desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid").post();

        basePageSteps.onAuthPage().input("Номер телефона").sendKeys("9111111111");
        basePageSteps.onAuthPage().input("Код из смс", "1234");
    }
}
