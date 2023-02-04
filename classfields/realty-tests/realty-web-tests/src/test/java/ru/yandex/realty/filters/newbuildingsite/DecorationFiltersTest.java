package ru.yandex.realty.filters.newbuildingsite;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;


@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DecorationFiltersTest {

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
    public String becameButton;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Чистовая отделка", "Чистовая", "CLEAN"},
                {"Черновая отделка", "Черновая", "ROUGH"},
                {"Отделка под ключ", "Под ключ", "TURNKEY"},
        });
    }

    @Description("В МОК НАДО ЧТО-ТО ДОБАВИТЬ.")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка тип отделки")
    public void shouldSeeBathroomButton() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        newBuildingSteps.resize(1400, 1600);
        urlSteps.testing().newbuildingSiteMock().queryParam("decoration", expected).open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().button(becameButton)
                .waitUntil(isDisplayed()).click();
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().filterPopup().item(label)
                .should(isChecked()).should(hasClass(not(containsString("item_empty"))));
    }
}
