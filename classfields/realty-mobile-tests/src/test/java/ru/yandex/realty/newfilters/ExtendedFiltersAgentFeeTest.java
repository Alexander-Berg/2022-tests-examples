package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KUPIT_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.SNYAT_OPTION;

@DisplayName("Расширенные фильтры. Агентская комиссия")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersAgentFeeTest {

    private static final String WITHOUT_COMISSION = "Без комиссии агенту";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Комиссия агенту»")
    public void shouldSeeAgentFeeInUrl() {
        basePageSteps.selectOption(
                basePageSteps.onMobileMainPage().extendFilters().selector(KUPIT_OPTION), SNYAT_OPTION);
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().button(WITHOUT_COMISSION));
        basePageSteps.onMobileMainPage().extendFilters().button(WITHOUT_COMISSION).click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(SNYAT).path(KVARTIRA).path("/bez-komissii/").shouldNotDiffWithWebDriverUrl();
    }
}
