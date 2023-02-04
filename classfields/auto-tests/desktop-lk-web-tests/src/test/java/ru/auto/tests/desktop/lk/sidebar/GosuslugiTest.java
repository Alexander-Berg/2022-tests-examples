package ru.auto.tests.desktop.lk.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Госуслуги")
@Feature(LK)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class GosuslugiTest {

    private static final String UNATTACHED_TEXT = "Привязать";
    private static final String UNTRUSTED_TEXT = "Подтвердить";
    private static final String UNATTACHED_URL = "https://auth.test.avto.ru/social/gosuslugi/";
    private static final String UNTRUSTED_URL = "https://www.gosuslugi.ru/help/faq/login/2003";
    private static final String UNATTACHED_SHOW = "{\"cars\":{\"sales\":{\"advantages\":{\"show-request\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNATTACHED_SHOW_POPUP = "{\"cars\":{\"sales\":{\"advantages\":{\"show-request-popup\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNATTACHED_TAP = "{\"cars\":{\"sales\":{\"advantages\":{\"tap-request\":{\"add_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_SHOW = "{\"cars\":{\"sales\":{\"advantages\":{\"show-request\":{\"verify_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_SHOW_POPUP = "{\"cars\":{\"sales\":{\"advantages\":{\"show-request-popup\":{\"verify_gosuslugi\":{}}}}}}";
    private static final String UNTRUSTED_TAP = "{\"cars\":{\"sales\":{\"advantages\":{\"tap-request\":{\"verify_gosuslugi\":{}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

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
    public String metricShow;

    @Parameterized.Parameter(4)
    public String metricShowPopup;

    @Parameterized.Parameter(5)
    public String metricTap;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop/User", UNATTACHED_TEXT, UNATTACHED_URL,
                        UNATTACHED_SHOW, UNATTACHED_SHOW_POPUP, UNATTACHED_TAP},
                {"desktop/UserGosuslugiUntrusted", UNTRUSTED_TEXT, UNTRUSTED_URL,
                        UNTRUSTED_SHOW, UNTRUSTED_SHOW_POPUP, UNTRUSTED_TAP}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub(mock)).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Госуслуги»")
    public void shouldSeeBlock() {
        basePageSteps.onLkSalesPage().sidebar().gosuslugiBlock().waitUntil(isDisplayed())
                .should(hasText("Аккаунт на Госуслугах\n" + text));

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricShow)));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка привязки/подтверждения в блоке")
    public void shouldClickBlockButton() {
        basePageSteps.onLkSalesPage().button(text).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricTap)));

        basePageSteps.switchToNextTab();

        urlSteps.shouldUrl(startsWith(url));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка привязки/подтверждения в попапе")
    public void shouldClickPopupButton() {
        basePageSteps.onLkSalesPage().sidebar().gosuslugiBlock().waitUntil(isDisplayed()).hover();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricShowPopup)));

        basePageSteps.onLkSalesPage().popup().button(text + " аккаунт").click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(metricTap)));

        basePageSteps.switchToNextTab();

        urlSteps.shouldUrl(startsWith(url));
    }
}
