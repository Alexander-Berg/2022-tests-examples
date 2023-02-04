package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SORTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PUBLISH_DATE_DESC_VALUE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(LISTING_FEATURE)
@Feature(SORTING_FEATURE)
@DisplayName("Формирование URL при сортировке листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingSortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String sortName;

    @Parameterized.Parameter(1)
    public String sortParamValue;

    @Parameterized.Parameters(name = "Сортировка «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Сначала свежие", SORT_BY_PUBLISH_DATE_DESC_VALUE},
                {"Сначала дешевле", SORT_BY_PRICE_ASC_VALUE},
                {"Сначала дороже", SORT_BY_PRICE_DESC_VALUE}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе сортировки листинга")
    public void shouldSeeSortValue() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().filters().sortButton().click();
        basePageSteps.onListingPage().popup().radioButtonWithLabel(sortName).click();

        urlSteps.queryParam(SORTING_PARAM, sortParamValue).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение типа сортировки в интерфейсе")
    public void shouldSeeSortName() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(SORTING_PARAM, sortParamValue).open();

        basePageSteps.onListingPage().filters().sortButton().should(hasText(sortName));
    }

}
