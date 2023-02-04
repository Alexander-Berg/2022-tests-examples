package ru.yandex.realty.filters.newbuildingsite;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.FROM;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.TO;

@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FloorNumberFiltersTest {

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

    @Before
    public void openNewBuildingPage() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        newBuildingSteps.resize(1400, 1600);
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Параметр «Этаж от»")
    public void shouldSeeFloorMinInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().floorFilter()
                .input(FROM, floorFrom + Keys.ENTER);
        urlSteps.queryParam("floorMin", floorFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Параметр «Этаж до»")
    public void shouldSeeFloorMaxInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().floorFilter().input(TO, floorTo + Keys.ENTER);
        urlSteps.queryParam("floorMax", floorTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Кнопка «Этаж от»")
    public void shouldSeeFloorMinButton() {
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("floorMin", floorFrom).open();
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().floorFilter()
                .input(FROM).should(hasValue(floorFrom));
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Кнопка «Этаж до»")
    public void shouldSeeFloorMaxButton() {
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("floorMax", floorTo).open();
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().floorFilter()
                .input(TO).should(hasValue(floorTo));
    }
}
