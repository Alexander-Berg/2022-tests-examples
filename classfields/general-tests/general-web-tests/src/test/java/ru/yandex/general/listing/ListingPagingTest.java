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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Area.area;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Constraint.constraint;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Coordinates.coordinates;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.GetOfferListingService.getOfferListingService;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.GreaterThan.greaterThan;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Parameter.parameter;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Request.request;
import static ru.yandex.general.beans.ajaxRequests.offerListingService.Toponyms.toponyms;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PAGING;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.QueryParams.DISABLE_DELIVERY_PARAM;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.TRUE_VALUE;
import static ru.yandex.general.step.AjaxProxySteps.GET_OFFER_LISTING_SERVICE;
import static ru.yandex.general.step.OfferAddSteps.NULL_STRING;

@Epic(LISTING_FEATURE)
@Feature(PAGING)
@DisplayName("Проверка пейджинга на листинге")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListingPagingTest {

    private static final String SEARCH_TEXT = "ноутбук apple";
    private static final String MIN_PRICE = "1";
    private static final String ELEKTRONIKA_ID = "elektronika_UhWUEm";
    private static final String MOSKVA_ID = "213";
    private static final String METRO_ID = "20475";
    private static final String DISTRICT_ID = "120543";
    private static final String LAT = "55.760572";
    private static final String LON = "37.622504";
    private static final String RADIUS = "1000";
    private static final int PXLS_TO_NEXT_PAGE = 3000;
    private static final String[] JSONPATHS_TO_IGNORE = {"pageToken", "request.area.coordinates"};
    private static final String[] JSONPATHS_TO_IGNORE_ADDRESS = {"pageToken", "request.area.toponyms"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        basePageSteps.resize(1920, 1080);
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга категории")
    public void shouldSeeGetListingCategorySecondPage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                        request().setCategoryId(ELEKTRONIKA_ID)
                                .setText("")
                                .setArea(area().setCoordinates(coordinates())
                                        .setToponyms(toponyms().setMetro(asList())
                                                .setDistricts(asList())
                                                .setRegion(MOSKVA_ID)))
                                .setParameters(asList())
                                .setLockedFields(asList())).toString()).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга текстового поиска")
    public void shouldSeeGetListingTextSearchSecondPage() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, "ноутбук apple").open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                        request().setCategoryId(NULL_STRING)
                                .setText(SEARCH_TEXT)
                                .setArea(area().setCoordinates(coordinates())
                                        .setToponyms(toponyms().setMetro(asList())
                                                .setDistricts(asList())
                                                .setRegion(MOSKVA_ID)))
                                .setParameters(asList())
                                .setLockedFields(asList())).toString()).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга категории с фильтрами")
    public void shouldSeeGetListingCategoryWithFiltersSecondPage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).path(STATE_NEW)
                .queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE)
                .queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(DISABLE_DELIVERY_PARAM, TRUE_VALUE).open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                                request().setCategoryId(ELEKTRONIKA_ID)
                                        .setText("")
                                        .setArea(area().setCoordinates(coordinates())
                                                .setToponyms(toponyms().setMetro(asList())
                                                        .setDistricts(asList())
                                                        .setRegion(MOSKVA_ID)))
                                        .setParameters(asList(
                                                parameter().setKey("price").setConstraint(
                                                        constraint().setGreaterThan(
                                                                greaterThan().setValue(Integer.valueOf(MIN_PRICE))
                                                                        .setOrEquals(true))),
                                                parameter().setKey("offer.state").setConstraint(
                                                        constraint().setOneOf(asList("new"))),
                                                parameter().setKey("disable_delivery").setConstraint(
                                                        constraint().setEqualTo(true))))
                                        .setLockedFields(asList()))
                        .setSorting(SORT_BY_PRICE_ASC_VALUE).toString()).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга категории с метро")
    public void shouldSeeGetListingCategoryWithMetroSecondPage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(METRO_ID_PARAM, METRO_ID).open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                        request().setCategoryId(ELEKTRONIKA_ID)
                                .setText("")
                                .setArea(area().setCoordinates(coordinates())
                                        .setToponyms(toponyms().setMetro(asList(METRO_ID))
                                                .setDistricts(asList())
                                                .setRegion(MOSKVA_ID)))
                                .setParameters(asList())
                                .setLockedFields(asList())).toString()).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга категории с районом")
    public void shouldSeeGetListingCategoryWithDistrictSecondPage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(DISTRICT_ID_PARAM, DISTRICT_ID).open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                        request().setCategoryId(ELEKTRONIKA_ID)
                                .setText("")
                                .setArea(area().setCoordinates(coordinates())
                                        .setToponyms(toponyms().setMetro(asList())
                                                .setDistricts(asList(DISTRICT_ID))
                                                .setRegion(MOSKVA_ID)))
                                .setParameters(asList())
                                .setLockedFields(asList())).toString()).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы листинга категории с координатами")
    public void shouldSeeGetListingCategoryWithCoordinatesSecondPage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(LATITUDE_PARAM, LAT)
                .queryParam(LONGITUDE_PARAM, LON).queryParam(GEO_RADIUS_PARAM, RADIUS).open();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_OFFER_LISTING_SERVICE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE_ADDRESS)
                .withRequestText(getOfferListingService().setPage(2).setLimit(30).setRegionId(MOSKVA_ID).setRequest(
                        request().setCategoryId(ELEKTRONIKA_ID)
                                .setText("")
                                .setArea(area().setCoordinates(coordinates()
                                                .setLatitude(Double.valueOf(LAT))
                                                .setLongitude(Double.valueOf(LON))
                                                .setRadiusMeters(Integer.valueOf(RADIUS)))
                                        .setToponyms(toponyms()))
                                .setParameters(asList())
                                .setLockedFields(asList())).toString()).shouldExist();
    }

}
