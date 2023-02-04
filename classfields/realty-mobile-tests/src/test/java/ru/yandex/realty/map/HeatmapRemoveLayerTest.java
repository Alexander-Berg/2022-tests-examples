package ru.yandex.realty.map;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.HEATMAPS;
import static ru.yandex.realty.mobile.page.MapPage.NO_LAYER;
import static ru.yandex.realty.step.UrlSteps.LAYER_URL_PARAM;

@Issue("VERTISTEST-1407")
@Epic(HEATMAPS)
@DisplayName("Тепловая карта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class HeatmapRemoveLayerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скрытие слоя тепловой карты")
    public void shouldNotSeeHeatmapLayer() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(LAYER_URL_PARAM, "transport").open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().heatmap().waitUntil((isDisplayed()));
        basePageSteps.onMobileMapPage().layers().click();
        basePageSteps.onMobileMapPage().spanLink(NO_LAYER).click();

        basePageSteps.onMobileMapPage().heatmap().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скрытие карты школ")
    public void shouldNotSeeSchoolmap() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(LAYER_URL_PARAM, "education").open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().schoolLegendTitle().waitUntil((isDisplayed()));
        basePageSteps.onMobileMapPage().layers().click();
        basePageSteps.onMobileMapPage().spanLink(NO_LAYER).click();

        basePageSteps.onMobileMapPage().schoolLegendTitle().should(not(isDisplayed()));
    }

}
