package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Сортировки листинга")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String sortType;

    @Parameterized.Parameter(1)
    public String value;

    @Parameterized.Parameters(name = "Сортировка «{0}»")
    public static Collection<Object[]> testParamsAmp() {
        return asList(new Object[][]{
                {"новые предложения", "DATE_DESC"},
                {"цена по возрастанию", "PRICE"},
                {"цена по убыванию", "PRICE_DESC"},
                {"площадь по возрастанию", "AREA"},
                {"площадь по убыванию", "AREA_DESC"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сортировки листинга")
    public void shouldSeeSortAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onAmpSaleAdsPage().ampSortSelect().click();
        basePageSteps.onAmpSaleAdsPage().ampSortOption(sortType).click();

        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(SORT_URL_PARAM, value)
                .shouldNotDiffWithWebDriverUrl();
    }
}
