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
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.FROM;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.TO;

@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class KitchenAreaFiltersTest {

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
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        newBuildingSteps.resize(1400, 1600);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь кухни «от»")
    public void shouldSeeKitchenAreaMinInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().areaKitchenFilter()
                .input(FROM, areaFrom + Keys.ENTER);
        urlSteps.queryParam("kitchenSpaceMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Площадь кухни «до»")
    public void shouldSeeKitchenAreaMaxInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().areaKitchenFilter()
                .input(TO, areaTo + Keys.ENTER);
        urlSteps.queryParam("kitchenSpaceMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка Площадь кухни «от»")
    public void shouldSeeKitchenAreaMinButton() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("kitchenSpaceMin", areaFrom).open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().areaKitchenFilter()
                .input(FROM).should(hasValue(areaFrom));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка Площадь кухни «до»")
    public void shouldSeeKitchenAreaMaxButton() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam("kitchenSpaceMax", areaTo).open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().areaKitchenFilter()
                .input(TO).should(hasValue(areaTo));
    }
}
