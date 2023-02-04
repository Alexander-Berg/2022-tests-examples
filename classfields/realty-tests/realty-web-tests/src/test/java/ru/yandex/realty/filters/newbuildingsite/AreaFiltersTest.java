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
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.FROM;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.TO;

@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AreaFiltersTest {

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

    @Before
    public void openNewBuildingPage() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь  «от»")
    public void shouldSeeAllAreaMinInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().areaFilter().input(FROM, areaFrom + Keys.ENTER);
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь  «до»")
    public void shouldSeeAllAreaMaxInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().areaFilter().input(TO, areaTo + Keys.ENTER);
        urlSteps.queryParam("areaMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь  «от»")
    public void shouldSeeAllAreaMinButton() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("areaMin", areaFrom).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().areaFilter().input(FROM)
                .should(hasValue(areaFrom));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь  «до»")
    public void shouldSeeAllAreaMaxButton() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("areaMax", areaTo).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().areaFilter().input(TO)
                .should(hasValue(areaTo));
    }
}
