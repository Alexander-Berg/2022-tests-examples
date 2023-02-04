package ru.auto.tests.publicapi.testdata;


import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import ru.auto.tests.publicapi.model.AutoApiReview.UserOpinionEnum;

import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.model.AutoApiReview.UserOpinionEnum.DISLIKE;
import static ru.auto.tests.publicapi.model.AutoApiReview.UserOpinionEnum.LIKE;

/**
 * Created by vicdev on 06.10.17.
 */
public class TestData {

    private TestData() {
    }

    public static Object[] defaultCategories() {
        return new CategoryEnum[]{
                CARS,
                MOTO,
                TRUCKS
        };
    }

    public static Object[] provideMotoAndTruckCategories() {
        return new CategoryEnum[]{
                MOTO,
                TRUCKS
        };
    }

    public static Object[][] defaultOffersByCategories() {
        return new Object[][]{
                //base
                {CARS, "offers/cars.ftl"},
                {MOTO, "offers/moto.ftl"},
                {TRUCKS, "offers/trucks.ftl"},
                //full
                {CARS, "offers/full_cars_offer.ftl"},
                {MOTO, "offers/atv_offer.ftl"},
                {MOTO, "offers/motorcycle_offer.ftl"},
                {MOTO, "offers/scooters_offer.ftl"},
                {MOTO, "offers/snowmobile_offer.ftl"},
                {TRUCKS, "offers/artic_offer.ftl"},
                {TRUCKS, "offers/bus_offer.ftl"},
                {TRUCKS, "offers/lcv_offer.ftl"},
                {TRUCKS, "offers/trailer_offer.ftl"},
                {TRUCKS, "offers/truck_offer.ftl"},
                {TRUCKS, "offers/swap_body_offer.ftl"},
        };
    }

    public static Object[] defaultReviewsByCategories() {
        return new Object[]{
                "reviews_drafts/cars_review.json",
                "reviews_drafts/moto_atv_review.json",
                "reviews_drafts/moto_motorcycle_review.json",
                "reviews_drafts/moto_scooter_review.json",
                "reviews_drafts/moto_snowmobile_review.json",
                "reviews_drafts/truck_argicultural_review.json",
                "reviews_drafts/truck_atric_review.json",
                "reviews_drafts/truck_bulldozers_review.json",
                "reviews_drafts/truck_construction_review.json",
                "reviews_drafts/truck_crane_review.json",
                "reviews_drafts/truck_cranehydraulics_review.json",
                "reviews_drafts/truck_dredge_review.json",
                "reviews_drafts/truck_lcv_review.json",
                "reviews_drafts/truck_municipal_review.json",
                "reviews_drafts/truck_swapbody_review.json",
                "reviews_drafts/truck_trailer_review.json"};
    }

    public static Object[] defaultEvents() {
        return new Object[]{
                "events/default_events.json"};
    }

    public static Object[][] defaultOffersByBaseCategories() {
        return new Object[][]{
                {CARS, "offers/full_cars_offer.ftl"},
                {MOTO, "offers/motorcycle_offer.ftl"},
                {TRUCKS, "offers/bus_offer.ftl"},
        };
    }

    public static Object[][] defaultCarsSortsWithDirection() {
        return new Object[][]{
                {"fresh_relevance_1", "-", "desc"},
                {"cr_date", "-", "desc"},
                {"cr_date", "-", "asc"},
                {"km_age", "-", "asc"},
                {"price", "-", "desc"},
                {"price", "-", "asc"},
                {"year", "-", "asc"},
                {"year", "-", "desc"}
        };
    }

    public static Object[] invalidPaginations() {
        return new Integer[]{
                0, -1};
    }

    public static Object[] defaultOpinions() {
        return new UserOpinionEnum[]{
                LIKE,
                DISLIKE
        };
    }

    public static Object[] trueFalse() {
        return new Boolean[]{
                true,
                false
        };
    }

    public static Object[] provideToken() {
        return new String[]{
                "Vertis android-5442cd88c413ada3ce3d36a3d8061fb7",
                "Vertis ios-62ca2575df9c74b3958d118afcbb7602",
        };
    }

    public static Object[] defaultCreditApplications() {
        return new String[]{
            "shark/credit_applications.json"
        };
    }

    public static Object[] sharkVtbMaResponses() {
        return new String[]{
            "shark/vtb_ma_response.xml"
        };
    }

    public static Object[] sharkVtbStatusResponses() {
        return new String[]{
            "shark/vtb_status_response.xml"
        };
    }

    public static Object[] sharkSberbankStatusResponses() {
        return new String[]{
            "shark/sberbank_status_update.json"
        };
    }
}
