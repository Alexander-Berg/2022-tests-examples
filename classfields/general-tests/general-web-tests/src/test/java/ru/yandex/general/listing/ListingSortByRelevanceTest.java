package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SORTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(LISTING_FEATURE)
@Feature(SORTING_FEATURE)
@DisplayName("Тесты для дефолтной сортировки «По актуальности»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ListingSortByRelevanceTest {

    private static final String BY_RELEVANCE = "По актуальности";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе сортировки листинга «По актуальности»")
    public void shouldSeeByRelevanceUrlParam() {
        urlSteps.testing().path(ELEKTRONIKA).queryParam(REGION_ID_PARAM, MOSCOW_ID_VALUE)
                .queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE).open();
        basePageSteps.onListingPage().filters().sortButton().click();
        basePageSteps.onListingPage().popup().radioButtonWithLabel(BY_RELEVANCE).click();

        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение дефолтного типа сортировки в интерфейсе")
    public void shouldSeeDefaultSortName() {
        urlSteps.testing().path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().filters().sortButton().should(hasText(BY_RELEVANCE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение типа сортировки «По актуальности» в интерфейсе")
    public void shouldSeeByRelevanceSortName() {
        urlSteps.testing().path(ELEKTRONIKA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).open();

        basePageSteps.onListingPage().filters().sortButton().should(hasText(BY_RELEVANCE));
    }

}
