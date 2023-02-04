package ru.auto.tests.desktop.mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.mock.beans.carfaxReport.AutoruOffers;
import ru.auto.tests.desktop.mock.beans.carfaxReport.Offer;
import ru.auto.tests.desktop.mock.beans.carfaxReport.Record;
import ru.auto.tests.desktop.mock.beans.carfaxReport.VehiclePhotos;
import ru.auto.tests.desktop.mock.beans.photos.Photo;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.AutoruOffers.autoruOffers;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.Commentable.commentable;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.CommentsInfo.commentsInfo;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.Header.header;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.Record.record;
import static ru.auto.tests.desktop.mock.beans.carfaxReport.VehiclePhotos.vehiclePhotos;
import static ru.auto.tests.desktop.utils.Utils.getJsonByPath;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.auto.tests.desktop.utils.Utils.getResourceAsString;

public class MockCarfaxReport {

    public static final String BOUGHT_REPORT_RAW_EXAMPLE = "mocksConfigurable/carfax/CarfaxBoughtReportRawExample.json";
    public static final String REPORT_EXAMPLE = "mocksConfigurable/carfax/CarfaxReportExample.json";

    public static final String VEHICLE_PHOTO_HD = "mocksConfigurable/carfax/VehiclePhotosRecordPhotoHD.json";
    public static final String VEHICLE_PHOTO = "mocksConfigurable/carfax/VehiclePhotosRecordPhoto.json";
    public static final String AUTORU_OFFER_PHOTO_HD = "mocksConfigurable/carfax/AutoruOfferPhotoHD.json";
    public static final String AUTORU_OFFER_PHOTO = "mocksConfigurable/carfax/AutoruOfferPhoto.json";
    public static final String AUTORU_OFFER_EXAMPLE = "mocksConfigurable/carfax/AutoruOffer.json";

    private static final String REPORT = "report";
    private static final String AUTORU_OFFERS = "autoru_offers";
    private static final String VEHICLE_PHOTOS = "vehicle_photos";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockCarfaxReport(String pathToTemplate) {
        this.body = getJsonByPath(pathToTemplate);
    }

    public static MockCarfaxReport boughtReportRawExample() {
        return new MockCarfaxReport(BOUGHT_REPORT_RAW_EXAMPLE);
    }

    public static MockCarfaxReport reportExample() {
        return new MockCarfaxReport(REPORT_EXAMPLE);
    }

    @Step("Добавляем is_favorite = «{isFavorite}»")
    public MockCarfaxReport setIsFavorite(boolean isFavorite) {
        body.getAsJsonObject("raw_report").getAsJsonObject("report_offer_info").addProperty("is_favorite", isFavorite);
        return this;
    }

    @Step("Добавляем vas_sell_time = «{daysCount}»")
    public MockCarfaxReport setSellTimeDays(int daysCount) {
        body.getAsJsonObject(REPORT).getAsJsonObject("sell_time").addProperty("vas_sell_time", daysCount);
        return this;
    }

    @Step("Добавляем offer_id = «{offerId}»")
    public MockCarfaxReport setOfferId(int offerId) {
        body.getAsJsonObject(REPORT).getAsJsonObject("report_offer_info").addProperty("offer_id", offerId);
        return this;
    }

    @Step("Добавляем «autoru_offers» в мок отчёта")
    public MockCarfaxReport setAutoruOffers(AutoruOffers autoruOffers) {
        body.getAsJsonObject(REPORT).add(AUTORU_OFFERS, getJsonObject(autoruOffers));
        return this;
    }

    @Step("Убираем «autoru_offers» из мока отчёта")
    public MockCarfaxReport removeAutoruOffers() {
        body.getAsJsonObject(REPORT).remove(AUTORU_OFFERS);
        return this;
    }

    @Step("Добавляем «vehicle_photos» в мок отчёта")
    public MockCarfaxReport setVehiclePhotos(VehiclePhotos vehiclePhotos) {
        body.getAsJsonObject(REPORT).add(VEHICLE_PHOTOS, getJsonObject(vehiclePhotos));
        return this;
    }

    @Step("Убираем «vehicle_photos» из мока отчёта")
    public MockCarfaxReport removeVehiclePhotos() {
        body.getAsJsonObject(REPORT).remove(VEHICLE_PHOTOS);
        return this;
    }

    public static AutoruOffers getAutoruOffers(Offer... offers) {
        return autoruOffers()
                .setHeader(
                        header().setTitle("Объявления на Авто.ру")
                                .setTimestampUpdate("1658333538391"))
                .setStatus("OK")
                .setOffers(asList(offers))
                .setRecordCount(asList(offers).size());
    }

    public static Offer getOffer() {
        return new Gson().fromJson(getResourceAsString(AUTORU_OFFER_EXAMPLE), Offer.class)
                .setOfferId(getRandomOfferId());
    }

    public static VehiclePhotos getVehiclePhotos(Record... records) {
        return vehiclePhotos()
                .setHeader(
                        header().setTitle("Фотографии автолюбителей")
                                .setTimestampUpdate("1658333538391"))
                .setStatus("OK")
                .setRecords(asList(records))
                .setRecordCount(asList(records).size());
    }

    public static Record getRecord() {
        return record()
                .setCommentsInfo(
                        commentsInfo().setCommentable(
                                commentable().setAddComment(false)
                                        .setBlockId("images:http://img01.avto-nomer.ru/150601/o/ru5478731.jpg-1433106000000")))
                .setDate("1433106000000");
    }

    public static List<Photo> getVehiclePhotos(int photoCount) {
        return getPhotos(photoCount, VEHICLE_PHOTO_HD);
    }

    public static List<Photo> getOfferPhotos(int photoCount) {
        return getPhotos(photoCount, AUTORU_OFFER_PHOTO_HD);
    }

    private static List<Photo> getPhotos(int photoCount, String photoPath) {
        List<Photo> photos = new ArrayList<>();

        for (int i = 0; i < photoCount; i++) {
            photos.add(getPhoto(photoPath));
        }

        return photos;
    }

    public static Photo getPhoto(String photoPath) {
        return new Gson().fromJson(getResourceAsString(photoPath), Photo.class);
    }

}
