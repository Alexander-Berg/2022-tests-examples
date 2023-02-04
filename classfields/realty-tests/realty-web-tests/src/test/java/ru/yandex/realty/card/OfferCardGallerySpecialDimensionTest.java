package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT_WITH_NARROW_PHOTO;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Скриншот фулскрин галереи со специальным разрешением")
@Feature(OFFER_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-2093")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class OfferCardGallerySpecialDimensionTest {


    private static final int X_DIM = 1263;
    private static final int Y_DIM = 605;

    private MockOffer offer;
    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        basePageSteps.resize(X_DIM, Y_DIM);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фулскриншот планировки")
    public void shouldSeePlanPicScreenshot() {
        offer = mockOffer(SELL_APARTMENT);
        offerId = offer.getOfferId();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        String planUrl = "https:" + offer.getPlanPhotoUrl();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().openGallery();
        switchToGalleryNeededPic(planUrl);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().fsGallery());
        urlSteps.setProductionHost().open();
        basePageSteps.onOfferCardPage().openGallery();
        switchToGalleryNeededPic(planUrl);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().fsGallery());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Переключаемся в галерее на фото с планом")
    private void switchToGalleryNeededPic(String pic) {
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions().pollInSameThread()
                .pollInterval(1, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    basePageSteps.onOfferCardPage().fsGallery().mainSlider().click();
                    return basePageSteps.onOfferCardPage().fsGallery().mainSlider().img().getAttribute("src");
                }, equalTo(pic));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фулскриншот узкой фотографии")
    public void shouldSeeNarrowPicScreenshot() {
        offer = mockOffer(SELL_APARTMENT_WITH_NARROW_PHOTO);
        offerId = offer.getOfferId();
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();

        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().openGallery();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().fsGallery());
        urlSteps.setProductionHost().open();
        basePageSteps.onOfferCardPage().openGallery();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().fsGallery());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
