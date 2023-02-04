package ru.yandex.realty.touchgoals.favorites.add.card;

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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_GALLERY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_RELATED_OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_STICKY_RIGHT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mobile.page.OfferCardPage.ADD_TO_FAV;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.SimilarResponse.similarTemplate;

@DisplayName("Цель «favorites.add». Карточка оффера")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyMobileModule.class)
public class FavoritesOnOtherPlacementsCardGoalsTest {

    private static final String TWO_ROOMS = "2-комнатные";
    private MockOffer offer;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        offer = mockOffer(SELL_APARTMENT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Добавить в избранное» в галерее")
    public void shouldSeeGoalInGallery() {
        offer.setService(TURBOSALE).setExtImages();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();

        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().addToFavInGallery().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(TRUE)
                        .flatType(SECONDARY)
                        .placement(OFFER_GALLERY)
                        .pageType(CARD)
                        .vas(asList(TURBO))
                        .page(OFFER))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Добавить в избранное» плавающей панели")
    public void shouldSeeGoalInStickyRight() {
        offer.setService(RAISED).setPredictions();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();

        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().addToFavNavBar().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(TRUE)
                        .hasPlan(TRUE)
                        .flatType(SECONDARY)
                        .placement(OFFER_STICKY_RIGHT)
                        .pageType(CARD)
                        .vas(asList(RAISING))
                        .page(OFFER))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Добавить в избранное» в похожих офферах")
    public void shouldSeeGoalInRelatedOffers() {
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .similarStub(similarTemplate().offers(asList(offer)).build(), offer.getOfferId())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().similarOffers(), hasSize(greaterThan(0)));
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().firstSimilarOffer());
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(ADD_TO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(TRUE)
                        .flatType(SECONDARY)
                        .placement(OFFER_RELATED_OFFERS)
                        .pageType(CARD)
                        .page(OFFER))).shouldExist();
    }
}
