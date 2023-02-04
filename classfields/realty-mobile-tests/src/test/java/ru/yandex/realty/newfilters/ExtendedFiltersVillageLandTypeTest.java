package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.HOUSE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MO_RGID;
import static ru.yandex.realty.step.UrlSteps.OBJECT_TYPE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.VILLAGE_URL_PARAM;

@DisplayName("Фильтр поиска по коттеджным поселкам. Тип земли")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersVillageLandTypeTest {

    private static final String LAND_TYPE = "Тип земли";
    private static final String LAND_TYPE_URL_PARAM = "landType";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} -{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"МЖС", "MZHS"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Тип земли»")
    public void shouldSeeVillageLandType() {
        urlSteps.testing().path(Pages.FILTERS).queryParam("rgid" , MO_RGID).villageFiltersMobile().open();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName(LAND_TYPE));
        basePageSteps.onMobileMainPage().searchFilters().byName(LAND_TYPE).button(name).click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .queryParam(LAND_TYPE_URL_PARAM, expected).queryParam(CATEGORY_URL_PARAM, HOUSE_URL_PARAM)
                .queryParam(OBJECT_TYPE_URL_PARAM, VILLAGE_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }
}
