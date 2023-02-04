package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Проверяем запрос в метрику при покупке услуги «Поднятие в поиске» через попап относительной позиции")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MetricsFromRelativePositionPopupTest {

    private static final String METRIC_TEXT = "{\"vas\":{\"%s\":{\"lk\":{\"clicks\":{\"fresh\":{}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public SeleniumMockSteps browserMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salesMock;

    @Parameterized.Parameter(2)
    public String offerIdMock;

    @Parameterized.Parameter(3)
    public String categoryInMetric;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop-lk/UserOffersCarsActive", "desktop-lk/reseller/UserOffersCarsIdWithAllServices", "cars"},
                {MOTO, "desktop-lk/UserOffersMotoActive", "desktop-lk/reseller/UserOffersMotoIdWithAllServices", "moto"},
                {TRUCKS, "desktop-lk/UserOffersTrucksActive", "desktop-lk/reseller/UserOffersTrucksIdWithAllServices", "trucks"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserFavoriteReseller"),
                stub(salesMock),
                stub(offerIdMock),
                stub("desktop-lk/reseller/BillingAutoruPaymentInitRelativePosition"),
                stub("desktop-lk/reseller/BillingAutoruPaymentProcess"),
                stub("desktop-lk/BillingAutoruPayment")
        ).create();

        urlSteps.testing().path(MY).path(RESELLER).path(category).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Проверяем запрос в метрику при покупке услуги «Поднятие в поиске» через попап относительной позиции")
    public void shouldSeeMetricsRequest() {
        basePageSteps.onLkResellerSalesPage().getSale(0).button("Ниже, чем 27% похожих")
                .waitUntil(isDisplayed()).hover();
        basePageSteps.onLkResellerSalesPage().popup().buttonContains("Поднять в поиске за")
                .waitUntil(isDisplayed()).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo(format(METRIC_TEXT, categoryInMetric)))
        );
    }
}
