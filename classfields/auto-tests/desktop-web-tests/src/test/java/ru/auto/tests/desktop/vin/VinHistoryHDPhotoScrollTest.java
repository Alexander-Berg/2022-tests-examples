package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.element.history.ReportContent.HD_PHOTO;
import static ru.auto.tests.desktop.element.history.VinReport.BLOCK_AUTORU_OFFERS;
import static ru.auto.tests.desktop.element.history.VinReport.BLOCK_VEHICLE_PHOTOS;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.AUTORU_OFFER_PHOTO;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.AUTORU_OFFER_PHOTO_HD;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.VEHICLE_PHOTO;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.VEHICLE_PHOTO_HD;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getAutoruOffers;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getOffer;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getOfferPhotos;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getPhoto;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getRecord;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.getVehiclePhotos;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.reportExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CARFAX_OFFER_CARS_ID_RAW;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@Story("Блок «HD фотографий»")
@DisplayName("Блок «HD фотографий»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinHistoryHDPhotoScrollTest {

    private static final String offerId = getRandomOfferId();
    private static final String HD_PHOTO_TEMPLATE = "%d HD фотографи[ийя]";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser")
        );

        urlSteps.testing().path(HISTORY).path(offerId).path(SLASH);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Фотографии автолюбителей», нет «Объявления на Авто.ру», проверка каунтера и скрола")
    public void shouldSeeWithVehiclePhotosWithoutOffers() {
        int firstRecordPhotoCount = getRandomShortInt();
        int secondRecordPhotoCount = getRandomShortInt();

        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .removeAutoruOffers()
                                        .setVehiclePhotos(
                                                getVehiclePhotos(
                                                        getRecord().setGallery(
                                                                getVehiclePhotos(firstRecordPhotoCount)),
                                                        getRecord().setGallery(
                                                                getVehiclePhotos(secondRecordPhotoCount))))
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).waitUntil(hasText(matchesRegex(
                format(HD_PHOTO_TEMPLATE, firstRecordPhotoCount + secondRecordPhotoCount)))).click();

        assertThatWasScrollToBlock(BLOCK_VEHICLE_PHOTOS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Фотографии автолюбителей», частично HD, нет «Объявления на Авто.ру», проверка каунтера и скрола")
    public void shouldSeeWithVehiclePhotosPartlyHDWithoutOffers() {
        int hdPhotoCount = 2;

        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .removeAutoruOffers()
                                        .setVehiclePhotos(
                                                getVehiclePhotos(
                                                        getRecord().setGallery(asList(
                                                                getPhoto(VEHICLE_PHOTO_HD),
                                                                getPhoto(VEHICLE_PHOTO))),
                                                        getRecord().setGallery(asList(
                                                                getPhoto(VEHICLE_PHOTO))),
                                                        getRecord().setGallery(asList(
                                                                getPhoto(VEHICLE_PHOTO_HD)))))
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).waitUntil(hasText(matchesRegex(
                format(HD_PHOTO_TEMPLATE, hdPhotoCount)))).click();

        assertThatWasScrollToBlock(BLOCK_VEHICLE_PHOTOS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет блока «HD фотографий», когда ни одной HD «Фотографии автолюбителей» и нет «Объявления на Авто.ру»")
    public void shouldSeeNoHDWithoutHDVehiclePhotosWithoutOffers() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .removeAutoruOffers()
                                        .setVehiclePhotos(
                                                getVehiclePhotos(
                                                        getRecord().setGallery(asList(
                                                                getPhoto(VEHICLE_PHOTO),
                                                                getPhoto(VEHICLE_PHOTO))),
                                                        getRecord().setGallery(asList(
                                                                getPhoto(VEHICLE_PHOTO)))))
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Объявления на Авто.ру», нет «Фотографии автолюбителей», проверка каунтера и скрола")
    public void shouldSeeWithOffersWithoutVehiclePhotos() {
        int firstOfferPhotoCount = getRandomShortInt();
        int secondOfferPhotoCount = getRandomShortInt();
        int offersMainPhotoCount = 2;

        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .setAutoruOffers(
                                                getAutoruOffers(
                                                        getOffer().setPhotos(
                                                                getOfferPhotos(firstOfferPhotoCount)),
                                                        getOffer().setPhotos(
                                                                getOfferPhotos(secondOfferPhotoCount))))
                                        .removeVehiclePhotos()
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).waitUntil(hasText(matchesRegex(
                        format(HD_PHOTO_TEMPLATE, firstOfferPhotoCount + secondOfferPhotoCount + offersMainPhotoCount))))
                .click();

        assertThatWasScrollToBlock(BLOCK_AUTORU_OFFERS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Объявления на Авто.ру», частично HD, нет «Фотографии автолюбителей», проверка каунтера и скрола")
    public void shouldSeeWithOffersPartialyHDWithoutVehiclePhotos() {
        int hdPhotoCount = 5;

        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .setAutoruOffers(
                                                getAutoruOffers(
                                                        getOffer().setPhoto(
                                                                        getPhoto(AUTORU_OFFER_PHOTO_HD))
                                                                .setPhotos(asList(
                                                                        getPhoto(AUTORU_OFFER_PHOTO),
                                                                        getPhoto(AUTORU_OFFER_PHOTO_HD))),
                                                        getOffer().setPhoto(
                                                                        getPhoto(AUTORU_OFFER_PHOTO))
                                                                .setPhotos(asList(
                                                                        getPhoto(AUTORU_OFFER_PHOTO),
                                                                        getPhoto(AUTORU_OFFER_PHOTO))),
                                                        getOffer().setPhoto(
                                                                        getPhoto(AUTORU_OFFER_PHOTO_HD))
                                                                .setPhotos(asList(
                                                                        getPhoto(AUTORU_OFFER_PHOTO_HD),
                                                                        getPhoto(AUTORU_OFFER_PHOTO_HD)))))
                                        .removeVehiclePhotos()
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).waitUntil(hasText(matchesRegex(
                        format(HD_PHOTO_TEMPLATE, hdPhotoCount))))
                .click();

        assertThatWasScrollToBlock(BLOCK_AUTORU_OFFERS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет блока «HD фотографий», когда ни одной HD «Объявления на Авто.ру» и нет «Фотографии автолюбителей»")
    public void shouldSeeNoHDWithoutHDOffersWithoutVehiclePhotos() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .setAutoruOffers(
                                                getAutoruOffers(
                                                        getOffer().setPhoto(
                                                                        getPhoto(AUTORU_OFFER_PHOTO))
                                                                .setPhotos(asList(
                                                                        getPhoto(AUTORU_OFFER_PHOTO),
                                                                        getPhoto(AUTORU_OFFER_PHOTO))),
                                                        getOffer().setPhoto(
                                                                        getPhoto(AUTORU_OFFER_PHOTO))
                                                                .setPhotos(asList(
                                                                        getPhoto(AUTORU_OFFER_PHOTO),
                                                                        getPhoto(AUTORU_OFFER_PHOTO),
                                                                        getPhoto(AUTORU_OFFER_PHOTO)))))
                                        .removeVehiclePhotos()
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть «Объявления на Авто.ру» и «Фотографии автолюбителей», проверка каунтера и скрола")
    public void shouldSeeWithOffersWithVehiclePhotos() {
        int firstRecordPhotoCount = getRandomShortInt();
        int secondRecordPhotoCount = getRandomShortInt();
        int firstOfferPhotoCount = getRandomShortInt();
        int secondOfferPhotoCount = getRandomShortInt();
        int offersMainPhotoCount = 2;

        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .setAutoruOffers(
                                                getAutoruOffers(
                                                        getOffer().setPhotos(
                                                                getOfferPhotos(firstOfferPhotoCount)),
                                                        getOffer().setPhotos(
                                                                getOfferPhotos(secondOfferPhotoCount))))
                                        .setVehiclePhotos(
                                                getVehiclePhotos(
                                                        getRecord().setGallery(
                                                                getVehiclePhotos(firstRecordPhotoCount)),
                                                        getRecord().setGallery(
                                                                getVehiclePhotos(secondRecordPhotoCount))))
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).waitUntil(hasText(matchesRegex(
                        format(HD_PHOTO_TEMPLATE,
                                firstOfferPhotoCount + secondOfferPhotoCount + offersMainPhotoCount +
                                        firstRecordPhotoCount + secondRecordPhotoCount))))
                .click();

        assertThatWasScrollToBlock(BLOCK_AUTORU_OFFERS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет блока «HD фотографий» в отчете без «Объявления на Авто.ру» и без «Фотографии автолюбителей»")
    public void shouldSeeNoHDWithoutOffersWithoutVehiclePhotos() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .removeAutoruOffers()
                                        .removeVehiclePhotos()
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().contents().block(HD_PHOTO).should(not(isDisplayed()));
    }

    private void assertThatWasScrollToBlock(String blockId) {
        basePageSteps.waitScrollDownStopped();

        basePageSteps.assertThatPageBeenScrolledToElement(basePageSteps.onHistoryPage().vinReport().block(blockId));
    }

}
