package ru.auto.tests.publicapi.carfax;

import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;

import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;

public final class RawReportUtils {

    public final static String[] IGNORED_PATHS = new String[]{"report.header.timestamp_update",
            "report.pts_info.header.timestamp_update", "report.sources.header.timestamp_update",
            "report.autoru_offers.header.timestamp_update", "report.cheapening_graph.header.timestamp_update",
            "report.content.header.timestamp_update", "report.history.header.timestamp_update",
            "report.legal.header.timestamp_update", "report.pts_owners.header.timestamp_update",
            "report.mileages_graph.header.timestamp_update", "report.tax.header.timestamp_update",
            "report.total_auction.header.timestamp_update", "report.car_sharing.header.timestamp_update",
            "report.recalls.header.timestamp_update", "report.mileages_graph.mileages_graph_data.owners[*].end_date",
            "report.sell_time.header.timestamp_update", "report.vehicle.header.timestamp_update",
            "report.pledge.header.timestamp_update", "report.vehicle_photos.header.timestamp_update",
            "report.tech_inspection_block.header.timestamp_update", "report.health_score.header.timestamp_update",
            "report.customs.header.timestamp_update", "report.estimates.header.timestamp_update",
            "report.price_stats_graph.header.timestamp_update", "report.programs.header.timestamp_update",
            "report.constraints.header.timestamp_update", "report.autoru_offers.offers[*].time_of_expire",
            "report.history.owners[*].history_records[*].offer_record.time_of_expire", "report.purchase_timestamp",
            "report.vehicle_options.header.timestamp_update", "report.reviews.header.timestamp_update", "report.ya_images.header.timestamp_update"
    };

    static String createTestOffer(PublicApiAdaptor adaptor, AccountManager am) {
        Account offerOwnerAccount = am.create();
        String offerOwnerSessionId = adaptor.login(offerOwnerAccount).getSession().getId();
        return createTestOffer(adaptor, offerOwnerAccount, offerOwnerSessionId);
    }

    static String createTestOffer(PublicApiAdaptor adaptor, Account owner, String sessionId) {
        String offerId = adaptor.createOffer(owner.getLogin(), sessionId, CARS, "offers/offer_for_report_test.ftl").getOfferId();
        adaptor.waitOfferActivation(sessionId, CARS, offerId);
        return offerId;
    }
}
