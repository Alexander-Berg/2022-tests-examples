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
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры, селекты от/до")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardBaseFiltersSelectGroupsMotoTest {

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

    @Parameterized.Parameter
    public String paramName;

    @Parameterized.Parameter(1)
    public String paramValue;

    @Parameterized.Parameter(2)
    public String paramQueryName;

    @Parameterized.Parameter(3)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {6}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Год", "2016", "year", "2016"},
                {"Объем", "50 см³", "displacement", "50"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchMotoBreadcrumbs"),
                stub("desktop/SalonMoto"),
                stub("desktop/SearchMotoAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(MOTORCYCLE).path(ALL).path("motogarazh_moskva").open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект от")
    public void shouldSelectFrom() {
        basePageSteps.onDealerCardPage().filter().selectGroupItem(paramName, "от", paramValue);
        basePageSteps.onDealerCardPage().filter().select(paramValue).waitUntil(isDisplayed());

        urlSteps.path(SLASH).addParam(format("%s_from", paramQueryName), paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект до")
    public void shouldSelectTo() {
        basePageSteps.onDealerCardPage().filter().selectGroupItem(paramName, "от", paramValue);
        basePageSteps.onDealerCardPage().filter().select(paramValue).waitUntil(isDisplayed());

        urlSteps.path(SLASH).addParam(format("%s_from", paramQueryName), paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }
}
