package ru.yandex.realty.filters.geoselector;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Location.MOSCOW_OBL;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;


@DisplayName("Фильтры: метро")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MetroFiltersTransportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Parameterized.Parameter
    public String transport;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Пешком", "ON_FOOT"},
                {"На транспорте", "ON_TRANSPORT"}
        });
    }

    @Before
    public void openGeoSelectorPopup() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSCOW_OBL.getPath()).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        user.onOffersSearchPage().geoSelectorPopup().waitUntil(isDisplayed());
        user.onOffersSearchPage().geoSelectorPopup().tab("Метро").click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Видим время до метро и тип транспорта в урле")
    public void shouldSeeTimeToMetroInUrl() {
        user.onBasePage().geoSelectorPopup().timeToMetroInput().click();
        user.onBasePage().selectPopup().item("До 30 мин").waitUntil(isDisplayed()).click();
        user.onBasePage().geoSelectorPopup().metroTransport().click();
        user.onBasePage().selectPopup().item(transport).waitUntil(isDisplayed()).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.queryParam("timeToMetro", "30").queryParam("metroTransport", expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
