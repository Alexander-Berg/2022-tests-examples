package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры, селекты от/до")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardBaseFiltersSelectGroupsTrucksTest {

    private static final String PARAM_NAME = "Год";
    private static final String PARAM_QUERY_NAME = "year";
    private static final String VALUE = "2020";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SalonTrucks"),
                stub("desktop/SearchTrucksAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("sollers_finans_moskva").open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект от")
    public void shouldSelectFrom() {
        basePageSteps.onDealerCardPage().filter().selectGroupItem(PARAM_NAME, "от", VALUE);
        basePageSteps.onDealerCardPage().filter().select(VALUE).waitUntil(isDisplayed());

        urlSteps.path(SLASH).addParam(format("%s_from", PARAM_QUERY_NAME), VALUE).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект до")
    public void shouldSelectTo() {
        basePageSteps.onDealerCardPage().filter().selectGroupItem(PARAM_NAME, "от", VALUE);
        basePageSteps.onDealerCardPage().filter().select(VALUE).waitUntil(isDisplayed());

        urlSteps.path(SLASH).addParam(format("%s_from", PARAM_QUERY_NAME), VALUE).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }
}
