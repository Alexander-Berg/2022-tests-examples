package ru.yandex.realty.filters.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.JENKL;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.ADDRESS_INPUT;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 25.01.18
 */
@DisplayName("Расширенные фильтры поиска по новостройкам")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NameBuildingParamTest {

    private static final String SITE_ID = "166185";
    private static final String NAME = "Английский Квартал";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Параметр 'Название ЖК'")
    public void shouldSeeNameBuildingInUrl() throws IOException {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();

        user.onNewBuildingPage().filters().input(ADDRESS_INPUT).sendKeys(NAME);
        user.onNewBuildingPage().filters().suggest(NAME).click();

        String siteId = retrofitApiSteps.suggest(NAME, "587795", "SELL", "APARTMENT")
                .stream()
                .filter(res -> res.getLabel().contains(NAME))
                .findFirst().get()
                .getData().getParams().getSiteId().get(0);

        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.queryParam("siteId", siteId).queryParam(UrlSteps.SITE_NAME, NAME).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(JENKL)
    @DisplayName("Параметр 'Название ЖК' удаляется из урла")
    public void shouldNotSeeNameBuildingInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).queryParam("siteId", SITE_ID).open();
        user.onNewBuildingPage().filters().badge().should(isDisplayed()).click();
        user.onNewBuildingPage().filters().badges("Бунинские луга").clearGeo().click();
        user.onNewBuildingPage().filters().badge().should(not(exists()));
        user.onNewBuildingPage().filters().submitButton().click();

        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).toString());
    }
}
