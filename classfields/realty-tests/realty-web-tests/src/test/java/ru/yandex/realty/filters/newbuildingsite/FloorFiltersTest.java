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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FloorFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameter(2)
    public String value;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Не первый", "floorExceptFirst", "YES"},
                {"Не последний", "lastFloor", "NO"},
                {"Последний", "lastFloor", "YES"}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        newBuildingSteps.resize(1400, 1600);
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр  «Этаж»")
    public void shouldSeeFloorCheckboxFiltersInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.resize(1920, 3000);
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().button(label).click();
        urlSteps.queryParam(expected, value).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Чекбокс  этажей")
    public void shouldSeeFloorCheckboxButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(expected, value).open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().button(label).should(isChecked());
    }
}
