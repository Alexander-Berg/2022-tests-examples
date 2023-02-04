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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isActive;
import static ru.yandex.realty.mobile.page.OfferCardPage.CALL_BUTTON;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@Issue("VERTISTEST-1351")
@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class GalleryTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея открывается")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryOpen() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея закрывается")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryClose() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().offersGallery().closeCrossHeader().click();
        basePageSteps.onOfferCardPage().offersGallery().should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Позвонить» в галерее отображается")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryCallButton() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().offersGallery().link(CALL_BUTTON).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка добавить в «Избранное» ")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryFavButtonChecked() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().offersGallery().addToFav().should(not(isActive()));
        basePageSteps.onOfferCardPage().offersGallery().addToFav().click();
        basePageSteps.onOfferCardPage().offersGallery().addToFav().should(isActive());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка убрать из «Избранного» ")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryFavButtonUnChecked() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().offersGallery().addToFav().click();
        basePageSteps.onOfferCardPage().offersGallery().addToFav().waitUntil(isActive());
        basePageSteps.onOfferCardPage().offersGallery().addToFav().click();
        basePageSteps.onOfferCardPage().offersGallery().addToFav().should(not(isActive()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот галереи")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferGalleryScreenShot() {
        compareSteps
                .resize(380, 1500);
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onOfferCardPage().offerPhoto().click();
        basePageSteps.onOfferCardPage().offersGallery().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
