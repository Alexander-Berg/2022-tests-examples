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
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.element.FiltersPopup.METRO;
import static ru.yandex.general.mobile.element.FiltersPopup.STATION_OR_LINE;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе метро")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SubwayTest {

    private static final String PAVELECKAYA = "Павелецкая";
    private static final String PAVELECKAYA_ID = "20475";
    private static final String PROLETARSKAYA = "Пролетарская";
    private static final String PROLETARSKAYA_ID = "20503";
    private static final String AKADEMICHESKAYA = "Академическая";
    private static final String AKADEMICHESKAYA_ID = "20434";

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
        urlSteps.testing().path(MOSKVA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе станции метро в списке")
    public void shouldSeeSubwayUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().menuItem(PROLETARSKAYA).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(METRO_ID_PARAM, PROLETARSKAYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе станции метро из саджеста")
    public void shouldSeeSubwayUrlFromSuggest() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().input(STATION_OR_LINE).sendKeys(PAVELECKAYA);
        basePageSteps.onListingPage().popup().menuItem(PAVELECKAYA).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе станции метро по крестику в инпуте метро")
    public void shouldSeeUrlAfterClearSubwayInInput() {
        urlSteps.queryParam(METRO_ID_PARAM, PROLETARSKAYA_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).clearInput().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе станции метро по крестику в чипсине в попапе выбора метро")
    public void shouldSeeUrlAfterClearSubwayInChips() {
        urlSteps.queryParam(METRO_ID_PARAM, PROLETARSKAYA_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().chips(PROLETARSKAYA).reset().click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе станции метро в списке сняв чекбокс")
    public void shouldSeeUrlAfterClearSubwayChecbox() {
        urlSteps.queryParam(METRO_ID_PARAM, AKADEMICHESKAYA_ID).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().menuItem(AKADEMICHESKAYA).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

}
