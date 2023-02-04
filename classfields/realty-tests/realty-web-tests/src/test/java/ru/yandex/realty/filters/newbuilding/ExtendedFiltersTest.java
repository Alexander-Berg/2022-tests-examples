package ru.yandex.realty.filters.newbuilding;

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
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.io.IOException;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.DEVELOPER_FIELD;


@DisplayName("Расширенные фильтры поиска по новостройкам")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void openNewBuildingPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.resize(1920, 3000);
        basePageSteps.onNewBuildingPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь от»")
    public void shouldSeeAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onNewBuildingPage().extendFilters().byName("Площадь").input("от").sendKeys(areaFrom);
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь до»")
    public void shouldSeeAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onNewBuildingPage().extendFilters().byName("Площадь").input("до").sendKeys(areaTo);
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «этаж от»")
    public void shouldSeeFloorMinInUrl() {
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onNewBuildingPage().extendFilters().byName("Этаж").input("c").sendKeys(floorFrom);
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMin", floorFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «этаж до»")
    public void shouldSeeFloorMaxInUrl() {
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onNewBuildingPage().extendFilters().byName("Этаж").input("по").sendKeys(floorTo);
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMax", floorTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Без апартаментов»")
    public void shouldSeeExceptApartmentsInUrl() {
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("Без апартаментов");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("apartments", "NO").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только апартаменты»")
    public void shouldSeeOnlyApartmentsInUrl() {
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("Только апартаменты");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/apartamenty/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Застройщик»")
    public void shouldSeeDeveloperInUrl() throws IOException {
        basePageSteps.onNewBuildingPage().extendFilters().byName(DEVELOPER_FIELD).input().sendKeys("Аб");
        basePageSteps.onNewBuildingPage().extendFilters().buildingNameFilter().suggestList()
                .waitUntil("", hasSize(greaterThan(1)), 30).get(0).click();

        String developerName = basePageSteps.onNewBuildingPage().extendFilters().byName(DEVELOPER_FIELD).input()
                .getAttribute("value");
        String id = retrofitApiSteps.developerList(developerName).get(0).getId();

        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA)
                .path(format("/z-absolyut-nedvizhimost-%s/", id)).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Застройщик» видим при переходе по урлу")
    public void shouldSeeDeveloperChecked() throws IOException {
        String developerName = "Абсолют Недвижимость";
        String id = retrofitApiSteps.developerList(developerName).get(0).getId();
        urlSteps.queryParam("developerId", id).open();
        basePageSteps.onNewBuildingPage().openExtFilter();
        basePageSteps.onNewBuildingPage().extendFilters().byName(DEVELOPER_FIELD).input()
                .should(hasValue(developerName));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Время до метро»: Пешком")
    public void shouldSeeDefaultToMetroOnFootInUrl() {
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("5 мин");
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("Пешком");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("timeToMetro", "5")
                .queryParam("metroTransport", "ON_FOOT")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Время до метро»: На транспорте")
    public void shouldSeeMetroOnTransportInUrl() {
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("5 мин");
        basePageSteps.onNewBuildingPage().extendFilters().selectButton("На транспорте");
        basePageSteps.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("timeToMetro", "5")
                .queryParam("metroTransport", "ON_TRANSPORT")
                .shouldNotDiffWithWebDriverUrl();
    }
}
