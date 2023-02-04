package ru.yandex.realty.filters.map.newbuilding;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.ADDRESS_INPUT;
import static ru.yandex.realty.step.UrlSteps.SITE_NAME;

@DisplayName("Карта. Расширенные фильтры поиска по новостройкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NameBuildingParamTest {

    private static final String SITE_ID = "166185";
    private static final String NAME = "Английский Квартал";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Название ЖК»")
    public void shouldSeeNameBuildingInUrl() throws IOException {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();

        basePageSteps.onMapPage().filters().input(ADDRESS_INPUT).sendKeys(NAME);
        basePageSteps.onMapPage().filters().suggest(NAME).click();

        String siteId = retrofitApiSteps.suggest(NAME, "587795", "SELL", "APARTMENT")
                .stream()
                .filter(res -> res.getLabel().contains(NAME))
                .findFirst().get()
                .getData().getParams().getSiteId().get(0);
        urlSteps.queryParam("siteId", siteId).queryParam(SITE_NAME, NAME).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Название ЖК» удаляется из урла")
    public void shouldNotSeeNameBuildingInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).queryParam("siteId", SITE_ID).open();
        basePageSteps.onMapPage().filters().badgesCounter().should(isDisplayed()).click();
        basePageSteps.onMapPage().filters().badges("Бунинские луга").clearGeo().click();
        basePageSteps.onMapPage().filters().badgesCounter().should(not(exists()));
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }
}
