package ru.yandex.realty.filters.map.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.NewBuildingSimplePointSearchTemplate.newBuildingSimplePointSearchTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.rules.MockRuleConfigurable.NB_ID;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_SITE_OFFER_STAT;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_SITE_PLAN_SEARCH;

@DisplayName("Карта. Расширенные фильтры поиска по новостройкам.")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SaveFiltersRoomsClickOfferTest {
    private static final String ACTIVE_POINT = "activePoint";
    private static final String ACTIVE_POINT_VALUE = "60.03535%2C30.225615";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String pathName;

    @Parameterized.Parameter(2)
    public List<String> paramValue;

    @Parameterized.Parameters(name = "{index}- при клике сохраняется {0} ")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Студия", "studiya/", asList("STUDIO")},
                {"Однокомнатная", "odnokomnatnaya/", asList("1")},
                {"Двухкомнатная", "dvuhkomnatnaya/", asList("2")},
                {"Трехкомнатная", "tryohkomnatnaya/", asList("3")},
                {"Четырех- и более", "4-i-bolee/", asList("PLUS_4")},
                {"Несколько", "/studiya,2,4-i-bolee-komnatnie", asList("STUDIO", "2", "PLUS_4")}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeRoomsInUrl() {
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(parseInt(NB_ID)).build())
                .getSiteOfferStat(NB_ID, PATH_TO_SITE_OFFER_STAT)
                .getSitePlanSearch(NB_ID, PATH_TO_SITE_PLAN_SEARCH)
                .newbuildingSimplePointSearchStub(newBuildingSimplePointSearchTemplate().setId(parseInt(NB_ID)).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                        mockSite(SITE_TEMPLATE).setId(parseInt(NB_ID)))).buildSite()).createWithDefaults();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(pathName).path(KARTA)
                .queryParam(ACTIVE_POINT, ACTIVE_POINT_VALUE).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().sidebar().newbuildingCard().jkLink());
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(UrlSteps.NB_MOCK_PATH)
                .ignoreParam("rgid");
        paramValue.forEach(value -> urlSteps.queryParam("roomsTotal", value));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }
}
