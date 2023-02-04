package ru.yandex.realty.filters.map.newbuilding;

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
import ru.yandex.realty.beans.SuggestItem;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.io.IOException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.DEVELOPER_FIELD;

@DisplayName("Карта. Расширенные фильтры поиска по новостройкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersDeveloperTest {

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
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Застройщик»")
    public void shouldSeeDeveloperInUrl() throws IOException {
        basePageSteps.onMapPage().extendFilters().byName(DEVELOPER_FIELD).input().sendKeys("Аб");
        basePageSteps.onMapPage().extendFilters().buildingNameFilter().suggestList()
                .waitUntil("", hasSize(greaterThan(1)), 30).get(0).click();

        String developerName = basePageSteps.onMapPage().extendFilters().byName(DEVELOPER_FIELD).input()
                .getAttribute("value");
        SuggestItem developer = retrofitApiSteps.developerList(developerName).get(0);
        urlSteps.queryParam("developerId", developer.getId()).queryParam("developerName", developer.getName())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Застройщик» видим при переходе по урлу")
    public void shouldSeeDeveloperChecked() throws IOException {
        String developerName = "Абсолют Недвижимость";
        String id = retrofitApiSteps.developerList(developerName).get(0).getId();
        urlSteps.queryParam("developerId", id).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().byName(DEVELOPER_FIELD).input()
                .should(hasValue(developerName));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «ЖК с закрытыми продажами»")
    public void shouldSeeOutdatedInUrl() {
        basePageSteps.onMapPage().extendFilters().checkButton("ЖК с закрытыми продажами");
        urlSteps.queryParam("showOutdated", UrlSteps.YES_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «ЖК с закрытыми продажами» есть попап подсказки")
    public void shouldSeeTypesBuildingInUrl() {
        basePageSteps.onMapPage().extendFilters().buttonWithHint("ЖК с закрытыми продажами").hint().hover();
        basePageSteps.onMapPage().openedPopup().waitUntil(isDisplayed());
        basePageSteps.onMapPage().openedPopup().should(hasText(
                "Новостройки, в которых все квартиры от застройщика проданы или продажи временно приостановлены"));
    }
}
