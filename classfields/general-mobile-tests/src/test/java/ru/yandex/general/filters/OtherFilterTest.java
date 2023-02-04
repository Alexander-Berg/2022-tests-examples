package ru.yandex.general.filters;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.FIND_OFFERS;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры категории")
@DisplayName("Фильтры. Категорийные")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OtherFilterTest {

    private static final String FIRST_CATEGORY = "4 ГБ";
    private static final String FIRST_URL_CATEGORY = "4-gb";
    private static final String SECOND_CATEGORY = "8 ГБ";
    private static final String SECOND_URL_CATEGORY = "8-gb";
    private static final String THIRD_CATEGORY = "16 ГБ";
    private static final String THIRD_URL_CATEGORY = "16-gb";

    private static final String FILTER_CATEGORY = "offer.attributes.operativnaya-pamyat_15938685_serJvE";
    private static final String CATEGORY_FILTER = "Оперативная память";

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
        basePageSteps.resize(375, 1500);
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Два фильтра через запятую")
    public void shouldSeeTwoCategoryFilter() {
        urlSteps.testing().path(NOUTBUKI)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, SECOND_URL_CATEGORY).open();
        basePageSteps.onListingPage().filter(format("%s, %s", FIRST_CATEGORY, SECOND_CATEGORY)).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Три фильтра через -> «+1» в кнопке")
    public void shouldSeeThreeCategoryFilter() {
        urlSteps.testing().path(NOUTBUKI)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, SECOND_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, THIRD_URL_CATEGORY).open();
        basePageSteps.onListingPage().filter(format("%s, %s +1", FIRST_CATEGORY, SECOND_CATEGORY))
                .should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("При добавлении еще одной категории «+1» -> «+2»")
    public void shouldSeeFourCategoryFilter() {
        urlSteps.testing().path(NOUTBUKI)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, SECOND_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, THIRD_URL_CATEGORY).open();
        basePageSteps.onListingPage().filter(format("%s, %s +1", FIRST_CATEGORY, SECOND_CATEGORY)).click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).item("6 ГБ").click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).button(FIND_OFFERS).click();
        basePageSteps.onListingPage().filter(format("%s, %s +2", FIRST_CATEGORY, "6 ГБ"))
                .should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Два фильтра в расширенных фильтрах")
    public void shouldSeeTwoCategoryInPopup() {
        urlSteps.testing().path(NOUTBUKI).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(CATEGORY_FILTER).click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).item(FIRST_CATEGORY).click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).item(SECOND_CATEGORY).click();
        basePageSteps.onListingPage().wrapper(CATEGORY_FILTER).button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s, %s", FIRST_CATEGORY, SECOND_CATEGORY)).should(isDisplayed());
    }
}
