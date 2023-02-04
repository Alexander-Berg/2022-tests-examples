package ru.yandex.general.suggest;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CHELYABINSK;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.mobile.element.FiltersPopup.DISTRICT_NAME;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе района")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class DistrictTest {

    private static final String TRAKTOROZAVODSKIY = "Тракторозаводский район";
    private static final String TRAKTOROZAVODSKIY_ID = "108161";
    private static final String LENINSKIY = "Ленинский район";
    private static final String LENINSKIY_ID = "108157";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CHELYABINSK);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе района")
    public void shouldSeeDistrictUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().menuItem(LENINSKIY).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(DISTRICT_ID_PARAM, LENINSKIY_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе района из саджеста")
    public void shouldSeeDistrictUrlFromSuggest() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().input(DISTRICT_NAME).sendKeys(TRAKTOROZAVODSKIY);
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.onListingPage().popup().menuItem(TRAKTOROZAVODSKIY).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(DISTRICT_ID_PARAM, TRAKTOROZAVODSKIY_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе района по крестику в инпуте метро")
    public void shouldSeeUrlAfterClearDistrictInInput() {
        urlSteps.queryParam(DISTRICT_ID_PARAM, TRAKTOROZAVODSKIY_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).clearInput().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(CHELYABINSK).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе района по крестику в чипсине в попапе выбора метро")
    public void shouldSeeUrlAfterClearDistrictInChips() {
        urlSteps.queryParam(DISTRICT_ID_PARAM, TRAKTOROZAVODSKIY_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().chips(TRAKTOROZAVODSKIY).reset().click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(CHELYABINSK).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе района в списке сняв чекбокс")
    public void shouldSeeUrlAfterClearDistrictChecbox() {
        urlSteps.queryParam(DISTRICT_ID_PARAM, TRAKTOROZAVODSKIY_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().menuItem(TRAKTOROZAVODSKIY).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(CHELYABINSK).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

}
