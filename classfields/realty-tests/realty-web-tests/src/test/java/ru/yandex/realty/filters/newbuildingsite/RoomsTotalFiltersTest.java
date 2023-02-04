package ru.yandex.realty.filters.newbuildingsite;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.NUMBER_OF_ROOMS;

@DisplayName("Карта сдачи на странице новостройки")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RoomsTotalFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String becameLabel;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Студия", "Студия", "STUDIO"},
                {"1 комната", "1 комн.", "1"},
                {"2 комнаты", "2 комн.", "2"},
                {"3 комнаты", "3 комн.", "3"},
                {"4 комнаты и более", "4+ комн.", "PLUS_4"},
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «Количество комнат».")
    public void shouldSeeRoomsTotalDateParamInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().select(NUMBER_OF_ROOMS, label, becameLabel);
        urlSteps.queryParam("roomsTotal", expected).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Кнопка «Количество комнат».")
    public void shouldSeeRoomsTotalDateButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam("roomsTotal", expected).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().button(becameLabel).should(isDisplayed());
    }
}
