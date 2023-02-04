package ru.yandex.realty.filters.map.offers;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedExtraFiltersAgentFeeTest {

    private static final String WITHOUT_COMISSION = "Без комиссии агенту";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Комиссия агенту»")
    public void shouldSeeAgentFeeInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(SNYAT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().checkButton(WITHOUT_COMISSION);
        basePageSteps.loaderWait();
        urlSteps.queryParam("hasAgentFee", "NO").shouldNotDiffWithWebDriverUrl();
    }
}
