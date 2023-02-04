package ru.yandex.realty.map;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mock.VillagePointSearchTemplate.villagePointSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Сниппеты на карте.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ClickVillagesSnippetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        mockRuleConfigurable.villagePointSearch(villagePointSearchTemplate().setId("200").build()).createWithDefaults();

        compareSteps.resize(380, 950);
        urlSteps.testing().path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KANTEMIROV)
    @DisplayName("Клик по сниппету коттеджного поселка")
    public void shouldSeeVillageMapOfferClick() {
        basePageSteps.onMobileMapPage().offer(FIRST).offerLink().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот сниппета коттеджного поселка")
    public void shouldSeeVillageMapOfferScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
