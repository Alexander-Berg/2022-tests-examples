package ru.auto.tests.desktop.marketingiframe;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Маркетинговый iframe")
@Feature(METRICS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarketingIframeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object> getParameters() {
        return asList(
                "",
                "/moskva/cars/all/",
                "/cars/used/sale/1076842087-f1e84/",
                "/cars/new/group/kia/optima/21342050-21342121/"
        );
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                        "desktop/OfferCarsUsedUser",
                        "desktop/SearchCarsBreadcrumbsMarkModelGroup",
                        "desktop/SearchCarsGroupContextGroup",
                        "desktop/SearchCarsGroupContextListing",
                        "desktop/SearchCarsGroupComplectations",
                        "desktop/ReferenceCatalogCarsComplectations",
                        "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                        "desktop/ReferenceCatalogCarsTechParam")
                .post();

        cookieSteps.deleteCookie("noads");
        urlSteps.testing().path(url).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение iframe на странице")
    public void shouldOpenMarketingIframe() {
        urlSteps.fromUri(basePageSteps.onBasePage().marketingIframe().getAttribute("src"))
                .addParam("_debug", "1").open();
        basePageSteps.onBasePage().title()
                .should(hasAttribute("textContent", "Marketing Auto.ru"));
    }
}