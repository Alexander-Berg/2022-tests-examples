package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры. Высота потолков")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersCellingHeightChpuTest {

    private static final String CELLING_HEIGHT = "Высота потолков";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Высота потолков»")
    public void shouldSeeCellingHeightInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(CELLING_HEIGHT));
        basePageSteps.onMobileMainPage().extendFilters().byName(CELLING_HEIGHT).button("от 3 м").click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path("s-vysokimi-potolkami/").shouldNotDiffWithWebDriverUrl();
    }
}
