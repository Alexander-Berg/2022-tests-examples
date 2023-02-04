package ru.yandex.arenda.steps;

import com.google.inject.Inject;

import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.pages.CalculatorCostPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.CalculatorCostPage.AREA_ID;
import static ru.yandex.arenda.pages.CalculatorCostPage.COSMETIC_RENOVATION;
import static ru.yandex.arenda.pages.CalculatorCostPage.FLOOR_ID;
import static ru.yandex.arenda.pages.CalculatorCostPage.ROOMS_ID;

public class CalculatorSteps extends MainSteps {

    @Inject
    public UrlSteps urlSteps;

    public static final String TEST_ADDRESS = "Россия, Санкт-Петербург, Невский проспект, 88";
    public static final String TEST_ROOMS = "Комнат: 2";
    public static final String TEST_ROOMS_TOUCH = "2";
    public static final String TEST_AREA = "50";
    public static final String TEST_FLOOR = "3";
    public static final String TEST_RENOVATION = COSMETIC_RENOVATION;

    public static final String ADDRESS_PARAM = "address";
    public static final String AREA_PARAM = "area";
    public static final String FLOOR_PARAM = "floor";
    public static final String RENOVATION_PARAM = "renovation";
    public static final String TEST_RENOVATION_VALUE = "RENOVATION_COSMETIC_REQUIRE";

    public void fillTestCalculator(String testAddress, String testRooms, String testArea, String testFloor,
                                   String testRenovation) {
        onCalculatorCostPage().inputId(ADDRESS_ID).sendKeys(testAddress);
        onCalculatorCostTouchPage().pickFirstFromSuggest();
        onCalculatorCostPage().selector(ROOMS_ID).click();
        onCalculatorCostPage().option(testRooms).click();
        onCalculatorCostPage().inputId(AREA_ID).sendKeys(testArea);
        onCalculatorCostPage().inputId(FLOOR_ID).sendKeys(testFloor);
        onCalculatorCostPage().renovationCheckbox(testRenovation).click();
    }

    public void fillTestCalculatorTouch(String testAddress, String testRooms, String testArea, String testFloor,
                                        String testRenovation) {
        onCalculatorCostTouchPage().inputId(ADDRESS_ID).sendKeys(testAddress);
        onCalculatorCostTouchPage().pickFirstFromSuggest();
        onCalculatorCostTouchPage().roomsCount(testRooms).click();
        onCalculatorCostTouchPage().inputId(AREA_ID).sendKeys(testArea);
        onCalculatorCostTouchPage().inputId(FLOOR_ID).sendKeys(testFloor);
        onCalculatorCostTouchPage().renovationCheckbox(testRenovation).click();
    }

    public void shouldSeeTestCalculatorUrl(String rooms, String address, String area, String floor, String renovation) {
        urlSteps.testing().path(KALKULATOR_ARENDY).path(rooms).queryParam(ADDRESS_PARAM, address)
                .queryParam(AREA_PARAM, area).queryParam(FLOOR_PARAM, floor).queryParam(RENOVATION_PARAM, renovation)
                .shouldNotDiffWithWebDriverUrl();
    }
}
