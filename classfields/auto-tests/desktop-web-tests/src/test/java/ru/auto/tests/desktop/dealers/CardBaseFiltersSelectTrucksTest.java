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

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры, селекты")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardBaseFiltersSelectTrucksTest {

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
    public String category;

    @Parameterized.Parameter(1)
    public String dealerCode;

    @Parameterized.Parameter(2)
    public String mark;

    @Parameterized.Parameter(3)
    public String section;

    @Parameterized.Parameter(4)
    public String paramName;

    @Parameterized.Parameter(5)
    public String paramValue;

    @Parameterized.Parameter(6)
    public String paramQueryName;

    @Parameterized.Parameter(7)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {6}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {TRUCK, "sollers_finans_moskva", "", ALL, "Тип кузова", "Автовоз", "truck_type", "AUTOTRANSPORTER"},
                {TRUCK, "sollers_finans_moskva", "", ALL, "Тип кабины", "2-х местная без спального", "cabin_key", "SEAT_2_WO_SLEEP"},
                {TRUCK, "sollers_finans_moskva", "", ALL, "Двигатель", "Бензин", "engine_type", "GASOLINE"},
                {TRUCK, "sollers_finans_moskva", "", ALL, "Коробка", "Механическая ", "transmission", "MECHANICAL"},
                {TRUCK, "sollers_finans_moskva", "", ALL, "Колёсн. ф-ла", "10x10", "wheel_drive", "WD_10x10"},
                {TRUCK, "sollers_finans_moskva", "", ALL, "Шасси", "Рессора-рессора", "suspension_chassis", "SPRING_SPRING"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SalonTrucks"),
                stub("desktop/SearchTrucksAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(category).path(section).path(dealerCode).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект")
    public void shouldSelect() {
        basePageSteps.onDealerCardPage().filter().selectItem(paramName, paramValue);
        basePageSteps.onDealerCardPage().filter().select(paramValue.replaceAll("\u00a0", "").trim())
                .should(isDisplayed()).click();
        urlSteps.path(mark).path("/").addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }
}