package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.page.MapPage.TRAVEL_TIME;

@DisplayName("Карта. Общее. Время на дорогу")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapCommuteTest {

    private static final String TEST_ADDRESS = "Красная площадь, 1";
    private static final String ZOOM_PARAM = "zoom";
    private static final String COMMUTE_ADDRESS_PARAM = "commuteAddress";
    private static final String COMMUTE_POINT_LATITUDE_PARAM = "commutePointLatitude";
    private static final String COMMUTE_POINT_LONGITUDE_PARAM = "commutePointLongitude";
    private static final String COMMUTE_TIME_PARAM = "commuteTime";
    private static final String COMMUTE_TRANSPORT_PARAM = "commuteTransport";

    private static final String DEFAULT_TIME_VALUE = "20";
    private static final String PUBLIC_VALUE = "PUBLIC";
    private static final String BY_FOOT_VALUE = "BY_FOOT";
    private static final String AUTO_VALUE = "AUTO";
    private static final String COMMUTE_POINT_LATITUDE_VALUE = "55.755245";
    private static final String COMMUTE_POINT_LONGITUDE_VALUE = "37.61778";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA);
        basePageSteps.resize(1920, 3000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим блок «Время на дорогу»")
    public void shouldSeeCommuteBlock() {
        urlSteps.open();
        basePageSteps.onMapPage().mapButton(TRAVEL_TIME).click();
        basePageSteps.onMapPage().commutePanel().should(isDisplayed());
        basePageSteps.onMapPage().commuteSuggest().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим адрес, видим урл")
    public void shouldSeeCommuteAddressUrl() {
        urlSteps.open();
        basePageSteps.onMapPage().mapButton(TRAVEL_TIME).click();
        basePageSteps.onMapPage().commuteSuggest().input().sendKeys(TEST_ADDRESS);
        basePageSteps.onMapPage().commuteSuggest().suggestElement(TEST_ADDRESS).click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим адрес точкой на карте, видим урл")
    public void shouldSeeCommuteAddressByPointUrl() {
        urlSteps.open();
        basePageSteps.onMapPage().mapButton(TRAVEL_TIME).click();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().map());
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreParam(COMMUTE_ADDRESS_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Время на дорогу» при переходе по урлу с параметрами")
    public void shouldSeeCommuteAddress() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).open();
        basePageSteps.onMapPage().commuteSuggest().input().should(hasValue(TEST_ADDRESS));
        basePageSteps.onMapPage().commutePanel().publicTransportButton()
                .should(hasClass(containsString("_item_active")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем «Время на дорогу» при переходе по урлу с параметрами")
    public void shouldClearCommuteAddress() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).open();
        basePageSteps.onMapPage().commuteSuggest().clearSuggest().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA).ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем тип на «Пешком» -> видим в урле")
    public void shouldChangeTypeCommuteToByFoot() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).open();
        basePageSteps.onMapPage().commutePanel().byFootButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_TRANSPORT_PARAM, BY_FOOT_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем тип на «Автомобиле» -> видим в урле")
    public void shouldChangeTypeCommuteToAuto() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).open();
        basePageSteps.onMapPage().commutePanel().autoTransportButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_TRANSPORT_PARAM, AUTO_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем тип на «Автомобиле» -> видим в урле")
    public void shouldChangeTypeCommuteToPublicTransport() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, AUTO_VALUE).open();
        basePageSteps.onMapPage().commutePanel().publicTransportButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_TRANSPORT_PARAM, PUBLIC_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем время")
    public void shouldChangeTimeCommute() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, AUTO_VALUE).open();
        basePageSteps.onMapPage().commutePanel().timeSelectButton().click();
        basePageSteps.onMapPage().commutePanel().timeSuggestItem("до 10 минут").click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, "10")
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_TRANSPORT_PARAM, AUTO_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем адрес нажатием на «Изменить точку на карте»")
    public void shouldChangeAddressCommute() {
        urlSteps
                .queryParam(COMMUTE_ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(COMMUTE_POINT_LATITUDE_PARAM, COMMUTE_POINT_LATITUDE_VALUE)
                .queryParam(COMMUTE_POINT_LONGITUDE_PARAM, COMMUTE_POINT_LONGITUDE_VALUE)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, BY_FOOT_VALUE).open();
        basePageSteps.onMapPage().commutePanel().spanLink("Изменить точку на карте").click();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().map());
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .ignoreMapCoordinate()
                .ignoreParam(ZOOM_PARAM)
                .ignoreParam(COMMUTE_POINT_LATITUDE_PARAM)
                .ignoreParam(COMMUTE_POINT_LONGITUDE_PARAM)
                .ignoreParam(COMMUTE_ADDRESS_PARAM)
                .queryParam(COMMUTE_TIME_PARAM, DEFAULT_TIME_VALUE)
                .queryParam(COMMUTE_TRANSPORT_PARAM, BY_FOOT_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
