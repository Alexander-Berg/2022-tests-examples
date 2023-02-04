package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.IS_EXACT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-1351")
public class FiltersToOfferTest {

    private MockOffer offer;

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
    public String url;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"/kupit/kvartira/dvuhkomnatnaya/"},
                {"/snyat/garazh/"},
                {"/kupit/garazh/?priceMin=100000"},
                {"/snyat/kommercheskaya-nedvizhimost/sklad/?priceType=PER_METER&pricingPeriod=PER_YEAR"},
                {"/kupit/kvartira/?newFlat=NO&parkingType=UNDERGROUND&showSimilar=NO"},
        });
    }

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build());
        urlSteps.setSpbCookie();
        urlSteps.fromUri(urlSteps.getMobileTesting() + "/sankt-peterburg" + url);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры протягиваются")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferCardOverListing() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.open();
        String offerId = basePageSteps.onMobileSaleAdsPage().getOfferId(FIRST);
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER).path(offerId).path("/")
                .ignoreParam(RGID).shouldNotDiffWithWebDriverUrl();
    }
}
