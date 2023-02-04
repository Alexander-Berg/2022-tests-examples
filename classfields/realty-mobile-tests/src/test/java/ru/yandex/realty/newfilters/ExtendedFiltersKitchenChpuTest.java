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


@DisplayName("Расширенные фильтры. Кухня")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersKitchenChpuTest {

    private static final String KITCHEN = "Кухня";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Кухня от 10 м²»")
    public void shouldSeeKitchenInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(KITCHEN));
        basePageSteps.onMobileMainPage().extendFilters().byName(KITCHEN).button("от 10 м²").click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path("/s-bolshoy-kuhney/").shouldNotDiffWithWebDriverUrl();
    }
}
