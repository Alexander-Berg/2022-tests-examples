package ru.yandex.realty.filters.newbuildingsite;

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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.rules.MockRuleConfigurable.NB_ID;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_SITE_OFFER_STAT;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_SITE_PLAN_SEARCH;


@DisplayName("Расширенные фильтры в окне попапа новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BathRoomNotExistFiltersTest {

    private static final String BATHROOM_UNIT = "bathroomUnit";
    private static final String SOME_BATHROOMS = "Несколько санузлов";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().deleteBathroomUnit(2).setNewbuildingId(parseInt(NB_ID)).build())
                .getSiteOfferStat(NB_ID, PATH_TO_SITE_OFFER_STAT)
                .getSitePlanSearch(NB_ID, PATH_TO_SITE_PLAN_SEARCH)
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(mockOffer(SELL_APARTMENT))).build())
                .createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
        newBuildingSteps.resize(1400, 1600);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Строка совместных санузлов замьючена")
    public void shouldSeeBathroomInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().button("Санузел").click();
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().filterPopup().item(SOME_BATHROOMS)
                .should(hasClass(containsString("item_empty")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Строка совместных санузлов чекнута, но замьючена")
    public void shouldSeeBathroomButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(BATHROOM_UNIT, "TWO_AND_MORE").open();
        basePageSteps.clickUntil(newBuildingSteps.onNewBuildingSitePage().cardFiltersBottom().showMoreParams(),
                newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock(), isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().button("Несколько")
                .waitUntil(isDisplayed()).click();
        newBuildingSteps.onNewBuildingSitePage().extendedFiltersBlock().filterPopup().item(SOME_BATHROOMS)
                .should(isChecked());
    }
}
