package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isActive;
import static ru.yandex.realty.mobile.page.OfferCardPage.ADD_TO_FAV;
import static ru.yandex.realty.mobile.page.OfferCardPage.DEL_FROM_FAV;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.SimilarResponse.similarTemplate;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class FavoritesTest {

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
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .similarStub(similarTemplate().offers(asList(offer)).build(),
                        offer.getOfferId()).createWithDefaults();
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавить в избранное в хедере")
    public void shouldSeeAddedToFavInHeader() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().addToFavNavBar());
        basePageSteps.onOfferCardPage().addToFavNavBar().click();
        basePageSteps.onOfferCardPage().addToFavNavBar().should(isActive());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убрать из избранного в хедере")
    public void shouldSeeRemovedToFavInHeader() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().addToFavNavBar());
        basePageSteps.onOfferCardPage().addToFavNavBar().click();
        basePageSteps.onOfferCardPage().addToFavNavBar().waitUntil(isActive());
        basePageSteps.onOfferCardPage().addToFavNavBar().click();
        basePageSteps.onOfferCardPage().addToFavNavBar().should(not(isActive()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавить в избранное внизу")
    public void shouldSeeAddedToFavBottom() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().button(ADD_TO_FAV));
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().button(ADD_TO_FAV));
        basePageSteps.onOfferCardPage().button(ADD_TO_FAV).click();
        basePageSteps.onOfferCardPage().button(DEL_FROM_FAV).should(isActive());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убрать из избранного внизу")
    public void shouldSeeRemovedToFavBottom() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().button(ADD_TO_FAV));
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().button(ADD_TO_FAV));
        basePageSteps.onOfferCardPage().button(ADD_TO_FAV).click();
        basePageSteps.onOfferCardPage().button(DEL_FROM_FAV).waitUntil(isActive());
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().button(DEL_FROM_FAV));
        basePageSteps.onOfferCardPage().button(DEL_FROM_FAV).click();
        basePageSteps.onOfferCardPage().button(ADD_TO_FAV).should(not(isActive()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавить в избранное в похожем оффере")
    public void shouldSeeAddedToFavSimilarOffer() {
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().similarOffers(), hasSize(greaterThan(0)));
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(ADD_TO_FAV).click();
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(DEL_FROM_FAV).should(isActive());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убрать из избранного в похожем оффере")
    public void shouldSeeRemovedToFavSimilarOffer() {
        basePageSteps.scrollingUntil(() -> basePageSteps.onOfferCardPage().similarOffers(), hasSize(greaterThan(0)));
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(ADD_TO_FAV).click();
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(DEL_FROM_FAV).waitUntil(isActive());
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(DEL_FROM_FAV).click();
        basePageSteps.onOfferCardPage().firstSimilarOffer().buttonWithTitle(ADD_TO_FAV).should(not(isActive()));
    }
}
