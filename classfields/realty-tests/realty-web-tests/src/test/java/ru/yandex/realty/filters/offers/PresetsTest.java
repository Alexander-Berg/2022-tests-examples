package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@Link("https://st.yandex-team.ru/VERTISTEST-2089")
@DisplayName("Пресеты под фильтрами")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PresetsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Before
    public void openSaleAdsPage() {
        mockRuleConfigurable.getFastLinksNgStub().createWithDefaults();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сравниваем скриншоты открытых пресетов")
    public void shouldSeeOpenedPresetsScreenshot() {
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().openedPresets());
        urlSteps.setProductionHost().open();
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().openedPresets());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сравниваем скриншоты панельки кнопок с группами пресетов")
    public void shouldSeeOpenedPresetsGroupsScreenshot() {
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().presetGroups());
        urlSteps.setProductionHost().open();
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().presetGroups());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сравниваем скриншоты свернутых пресетов")
    public void shouldSeeClosedPresetsScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().closedPresets());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOffersSearchPage().closedPresets());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по кнопке групп пресетов -> видимые пресеты становятся другими")
    public void shouldSeePresetGroups() {
        final String firstPreset = "Под ключ";
        final String secondPreset = "В кирпично-монолитном доме";
        final String groupButtonPreset = "Тип дома";
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        basePageSteps.onOffersSearchPage().openedPresets().preset(firstPreset).waitUntil(isDisplayed());
        basePageSteps.onOffersSearchPage().presetGroups().button(groupButtonPreset).click();
        basePageSteps.onOffersSearchPage().openedPresets().preset(firstPreset).should(not(isDisplayed()));
        basePageSteps.onOffersSearchPage().openedPresets().preset(secondPreset).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по пресету не развернутому")
    public void shouldSeeApplyInClosedPresets() {
        final String testPreset = "Под ключ";
        final String testPath = "/pod-kluch/";
        basePageSteps.onOffersSearchPage().closedPresets().preset(testPreset).click();
        urlSteps.path(testPath).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по пресету развернутому")
    public void shouldSeeApplyInOpenedPresets() {
        final String testPreset = "На вторичном рынке";
        final String groupButtonPreset = "Сделка";
        final String testPath = "/vtorichniy-rynok/";
        basePageSteps.onOffersSearchPage().closedPresets().showMoreButton().click();
        basePageSteps.onOffersSearchPage().presetGroups().button(groupButtonPreset).click();
        basePageSteps.onOffersSearchPage().openedPresets().preset(testPreset).click();
        urlSteps.path(testPath).shouldNotDiffWithWebDriverUrl();
    }
}
