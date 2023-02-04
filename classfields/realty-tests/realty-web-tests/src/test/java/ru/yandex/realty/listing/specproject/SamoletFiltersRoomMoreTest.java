package ru.yandex.realty.listing.specproject;

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

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@DisplayName("Лендинг Самолета. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SamoletFiltersRoomMoreTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;


    @Before
    public void before() {
        urlSteps.testing().path(SAMOLET).path(CATALOG);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «количество комнат» 2 подряд")
    public void shouldSeeTwoNearRooms() {
        urlSteps.open();
        user.onSamoletPage().searchFilters().button("2").click();
        user.onSamoletPage().searchFilters().button("3").click();
        urlSteps.path("/2,3-komnatnie/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «количество комнат» 2 не подряд")
    public void shouldSeeTwoNotNearRooms() {
        urlSteps.open();
        user.onSamoletPage().searchFilters().button("1").click();
        user.onSamoletPage().searchFilters().button("4").click();
        urlSteps.queryParam(UrlSteps.ROOMS_TOTAL_URL_PARAM, "1", "PLUS_4").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «количество комнат» 3 комнаты")
    public void shouldSeeThreeRooms() {
        urlSteps.open();
        user.onSamoletPage().searchFilters().button("1").click();
        user.onSamoletPage().searchFilters().button("2").click();
        user.onSamoletPage().searchFilters().button("3").click();
        urlSteps.queryParam(UrlSteps.ROOMS_TOTAL_URL_PARAM, "1", "2", "3").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр комнатности в урле")
    public void shouldSeeCheckedButtonsFromUrl() {
        urlSteps.path("/1,2-komnatnie/").open();
        user.onSamoletPage().searchFilters().button("1").should(isChecked());
        user.onSamoletPage().searchFilters().button("2").should(isChecked());
    }
}
