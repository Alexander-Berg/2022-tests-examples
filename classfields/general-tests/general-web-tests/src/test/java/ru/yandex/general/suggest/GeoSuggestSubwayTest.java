package ru.yandex.general.suggest;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе метро")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class GeoSuggestSubwayTest {

    private static final String PAVELECKAYA = "Павелецкая";
    private static final String PAVELECKAYA_ID = "20475";
    private static final String LINE_4 = "4";
    private static final String LINE_4_STATION_1 = "20316";
    private static final String LINE_4_STATION_2 = "20317";
    private static final String LINE_4_STATION_3 = "20345";
    private static final String LINE_4_STATION_4 = "20346";
    private static final String LINE_4_STATION_5 = "20350";
    private static final String LINE_4_STATION_6 = "20351";
    private static final String LINE_4_STATION_7 = "20352";
    private static final String LINE_4_STATION_8 = "101378";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе станции метро")
    public void shouldSeeSubwayUrl() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе линии метро")
    public void shouldSeeSubwayLineUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(LINE_4).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(METRO_ID_PARAM, LINE_4_STATION_1)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_2)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_3)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_4)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_5)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_6)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_7)
                .queryParam(METRO_ID_PARAM, LINE_4_STATION_8)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе метро по крестику в инпуте")
    public void shouldSeeUrlAfterClearSubway() {
        urlSteps.testing().path(MOSKVA).queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).open();
        basePageSteps.onListingPage().searchBar().clearGeoInput().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе метро сняв выбор станции в попапе")
    public void shouldSeeUrlAfterClearSubwayInPopup() {
        urlSteps.testing().path(MOSKVA).queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).open();
        basePageSteps.onListingPage().searchBar().button(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
