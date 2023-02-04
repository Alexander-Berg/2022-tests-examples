package ru.yandex.realty.filters.newbuildingsite;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.TO;

@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersTest {

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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сбросить параметры")
    public void shouldSeeFlushButtonClickInUrl() {
        newBuildingSteps.resize(1400, 1600);
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().floorFilter().input(TO, floorTo + Keys.ENTER);
        newBuildingSteps.onNewBuildingSitePage().spin().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("floorMax", floorTo).shouldNotDiffWithWebDriverUrl();
        newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().spanLink("Сбросить").click();
        newBuildingSteps.onNewBuildingSitePage().spin().waitUntil(not(isDisplayed()));
        urlSteps.testing().newbuildingSiteMock().shouldNotDiffWithWebDriverUrl();
    }
}
