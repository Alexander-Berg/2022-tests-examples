package ru.yandex.realty.newbuilding.flats;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RoomsFilterTest {

    private static final String ROOMS_TOTAL = "roomsTotal";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String option;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} - «{0}»")
    public static Collection<Object[]> categoryOfRealty() {
        return asList(new Object[][]{
                {"Студия", "STUDIO"},
                {"1", "1"},
                {"2", "2"},
                {"3", "3"},
                {"4", "PLUS_4"}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeRoomsButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(ROOMS_TOTAL, expected).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().filters());
        basePageSteps.onNewBuildingCardPage().filters().checkBox(option).should(isChecked());
        urlSteps.ignoreParam(RGID).shouldNotDiffWithWebDriverUrl();
    }
}
