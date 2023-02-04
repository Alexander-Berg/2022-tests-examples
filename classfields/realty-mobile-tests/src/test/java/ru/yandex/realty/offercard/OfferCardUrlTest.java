package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.TRYOHKOMNATNAYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.SimilarResponse.similarTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class OfferCardUrlTest {

    public static final String DOCS = "Образцы документов для сделок с недвижимостью";
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

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build());
        urlSteps.setMoscowCookie();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Оффер открывается на следующей вкладке")
    public void shouldSeeOfferCardOnNextTab() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        String offerId = basePageSteps.onMobileSaleAdsPage().getOfferId(FIRST);
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER).path(offerId).path("/")
                .ignoreParam(RGID)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Возвращаемся на листинг после перехода на оффер")
    public void shouldSeeReturnToListing() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().navButtonBackOnOffer().click();
        basePageSteps.waitUntilSeeTabsCount(1);
        basePageSteps.switchToTab(0);
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка «Предложения рядом» ведет на карту")
    public void shouldSeeNearOffersOnMap() {
        mockRuleConfigurable.createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollDown(1000);
        basePageSteps.onOfferCardPage().link("Предложения рядом").click();
        urlSteps.shouldCurrentUrlContains(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path(TRYOHKOMNATNAYA).path(KARTA).toString());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на документы")
    public void shouldSeeDocPage() {
        mockRuleConfigurable.createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().link(DOCS));
        basePageSteps.onOfferCardPage().link(DOCS).click();
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("кликаем на кнопку «Показать ещё» в списке похожих офферов")
    public void shouldSeeMoreSimilarOffers() {
        List<MockOffer> similarOffers = asList(offer, offer, offer, offer, offer);
        mockRuleConfigurable.similarStub(similarTemplate().offers(similarOffers).build(),
                offer.getOfferId())
                .createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().similarOffers(), hasSize(greaterThan(0)));
        int similarOffersInitialSize = basePageSteps.onOfferCardPage().similarOffers()
                .filter(AtlasWebElement::isDisplayed).size();
        basePageSteps.onOfferCardPage().similarOfferShowMore().click();
        basePageSteps.onOfferCardPage().similarOffers().filter(AtlasWebElement::isDisplayed)
                .should(hasSize(greaterThan(similarOffersInitialSize)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на похожий оффер")
    public void shouldSeeSimilarOffer() {
        List<MockOffer> similarOffers = asList(offer);
        mockRuleConfigurable.similarStub(similarTemplate().offers(asList(offer)).build(), offer.getOfferId())
                .createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().similarOffers(), hasSize(greaterThan(0)));
        basePageSteps.onOfferCardPage().similarOffers().get(0).offerLink().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER).path(similarOffers.get(0).getOfferId() + "/").ignoreParam("rgid")
                 .queryParam("utm_source", "similar_card")
                .queryParam("utm_medium", "related").queryParam("utm_campaign", "recommendations-default")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на оффер истории")
    public void shouldSeeHistoryOffer() {
        mockRuleConfigurable.createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().historyOffers(), hasSize(greaterThan(0)));
        basePageSteps.onOfferCardPage().historyOffers().get(FIRST).click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(offer.getOfferAddress()).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на кнопку «Показать ещё» в списке офферов истотрии")
    public void shouldSeeMoreHistoryOffers() {
        mockRuleConfigurable.createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().historyOfferShowMore());
        basePageSteps.onOfferCardPage().historyOfferShowMore().click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(offer.getOfferAddress()).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }
}
