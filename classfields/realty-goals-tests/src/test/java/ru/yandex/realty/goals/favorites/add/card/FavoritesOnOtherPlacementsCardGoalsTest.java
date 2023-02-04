package ru.yandex.realty.goals.favorites.add.card;

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
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_GALLERY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_STICKY_RIGHT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Цель «favorites.add». Карточка оффера")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class FavoritesOnOtherPlacementsCardGoalsTest {

    private MockOffer cardOffer;

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
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Добавить в избранное» в галерее")
    public void shouldSeeAddFavoritesGoalInCardInGallery() {
        cardOffer = mockOffer(SELL_APARTMENT)
                .setService(TURBOSALE)
                .setExtImages();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(cardOffer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(cardOffer).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(cardOffer.getOfferId()).open();
        basePageSteps.onOfferCardPage().galleryOpener().click();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().fsGallery().addToFavButtonGallery().click();
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
        cardOffer = mockOffer(RENT_APARTMENT).setPredictions();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(cardOffer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(cardOffer).build())
                .createWithDefaults();
        basePageSteps.resize(1200, 1800);
        urlSteps.testing().path(Pages.OFFER).path(cardOffer.getOfferId()).open();
        basePageSteps.scrollingUntil(() ->
                basePageSteps.onOfferCardPage().hideableBlock().addToFavButton(), isDisplayed());
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().hideableBlock().addToFavButton().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(RENT)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(TRUE)
                        .hasPlan(FALSE)
                        .placement(OFFER_STICKY_RIGHT)
                        .pageType(CARD)
                        .page(OFFER))).shouldExist();
    }
}