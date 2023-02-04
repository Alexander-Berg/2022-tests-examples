package ru.yandex.realty.developer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.beans.developerSearchQuery.DeveloperSearchQueryResponse.developerByTemplate;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.UrlSteps.DEVELOPER_ID_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Ссылка на карточку застройщика в листинге ЖК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NewbuildingListingUrlTest {

    private static final String ID = "2000";
    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика")
    public void shouldSeeDeveloperUrl() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(
                developerByTemplate().setId(ID).setName(NAME))
                .buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().link().should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA_I_MO).developerPath(NAME_TRANSLIT, ID).toString())));
    }
}
