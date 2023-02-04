package ru.auto.tests.desktop.gosuslugi;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.GOSUSLUGI;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - Госуслуги")
@Feature(GOSUSLUGI)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class SellerCardTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String UNATTACHED_TEXT = "Привяжите аккаунт Госуслуг\nПривяжите подтверждённый аккаунт Госуслуг, чтобы покупатели больше доверяли объявлению. Никаких дополнительных данных не покажем — покупатели увидят только специальный бейдж, что у вас есть проверенный аккаунт на Госуслугах.\nПривязать аккаунт";
    private static final String UNTRUSTED_TEXT = "Аккаунт не подтвержден\nПодтвердите аккаунт Госуслуг, чтобы покупатели больше доверяли объявлению. Никаких дополнительных данных не покажем — покупатели увидят только специальный бейдж, что у вас есть проверенный аккаунт на Госуслугах.\nПодтвердить аккаунт";
    private static final String UNATTACHED_URL = "https://auth.test.avto.ru/social/gosuslugi/";
    private static final String UNTRUSTED_URL = "https://www.gosuslugi.ru/help/faq/login/2003";
    private static final String UNATTACHED_BUTTON = "Привязать аккаунт";
    private static final String UNTRUSTED_BUTTON = "Подтвердить аккаунт";
    private static final String UNATTACHED_SHOW = "{\"cars\":{\"card\":{\"advantages\":{\"show-request\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNATTACHED_SHOW_POPUP = "{\"cars\":{\"card\":{\"advantages\":{\"show-request-popup\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNATTACHED_TAP = "{\"cars\":{\"card\":{\"advantages\":{\"tap-request\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_SHOW = "{\"cars\":{\"card\":{\"advantages\":{\"show-request\":{\"verify_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_SHOW_POPUP = "{\"cars\":{\"card\":{\"advantages\":{\"show-request-popup\":{\"verify_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_TAP = "{\"cars\":{\"card\":{\"advantages\":{\"tap-request\":{\"verify_gosuslugi\":{}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public SeleniumMockSteps browserMockSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mock;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameter(3)
    public String button;

    @Parameterized.Parameter(4)
    public String metricShow;

    @Parameterized.Parameter(5)
    public String metricShowPopup;

    @Parameterized.Parameter(6)
    public String metricTap;

    @Parameterized.Parameters(name = "name = {index}: {3}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop/User", UNATTACHED_TEXT, UNATTACHED_URL, UNATTACHED_BUTTON,
                        UNATTACHED_SHOW, UNATTACHED_SHOW_POPUP, UNATTACHED_TAP},
                {"desktop/UserGosuslugiUntrusted", UNTRUSTED_TEXT, UNTRUSTED_URL, UNTRUSTED_BUTTON,
                        UNTRUSTED_SHOW, UNTRUSTED_SHOW_POPUP, UNTRUSTED_TAP}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserOwner"),
                stub(mock)
        ).create();

        cookieSteps.setCookieForBaseDomain("calls-promote-closed", "true");

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Преимущество «Госуслуги» для продавца")
    public void shouldSeeBenefitGosuslugi() {
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricShow)));

        basePageSteps.onCardPage().benefits().benefit("Госуслуги").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText(text));

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricShowPopup)));

        basePageSteps.onCardPage().popup().button(button).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricTap)));

        basePageSteps.switchToNextTab();

        urlSteps.shouldUrl(startsWith(url));
    }
}
