package ru.yandex.realty.mappage;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@DisplayName("Карта. Общее. Тепловые карты")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeatMapHintPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ссылку на помощь в попапе подсказки")
    public void shouldSeePassOnHelp() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, "profitability").open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().heatMapHintIcon());
        basePageSteps.onMapPage().heatMapHintPopup().link().should(hasHref(
                equalTo("https://yandex.ru/support/realty/search/indicators.html#price__profitability")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("В попапе подсказки «Инфраструктуре» видим ссылку на карты")
    public void shouldSeePassMap() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, "infrastructure").open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().heatMapHintIcon());
        basePageSteps.onMapPage().heatMapHintPopup().link().should(hasHref(equalTo("https://maps.yandex.ru/")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("В попапе подсказки «Драйв» видим ссылку на драйв")
    public void shouldSeePassDrive() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, "carsharing").open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().heatMapHintIcon());
        basePageSteps.onMapPage().heatMapHintPopup().link().should(hasHref(equalTo("https://yandex.ru/drive/")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим скрин попапа подсказки")
    public void shouldSeeHeatMapHintPopupScreenshot() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UrlSteps.LAYER_URL_PARAM, "education").open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().heatMapHintIcon());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().heatMapHintPopup());

        urlSteps.setProductionHost().open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().heatMapHintIcon());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().heatMapHintPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
