package ru.yandex.realty.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Сортировки листинга")
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
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"новые предложения", "DATE_DESC"},
                {"цена по возрастанию", "PRICE"},
                {"цена по убыванию", "PRICE_DESC"},
                {"площадь по возрастанию", "AREA"},
                {"площадь по убыванию", "AREA_DESC"}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сортировки листинга")
    public void shouldSeeSort() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().sortSelect().click();
        basePageSteps.onMobileSaleAdsPage().sortOption(sortType).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(SORT_URL_PARAM, value)
                .shouldNotDiffWithWebDriverUrl();
    }
}
