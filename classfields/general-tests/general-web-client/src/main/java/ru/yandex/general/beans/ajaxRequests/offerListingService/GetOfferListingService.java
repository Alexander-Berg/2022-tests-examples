package ru.yandex.general.beans.ajaxRequests.offerListingService;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.ajaxRequests.ActivateOffers;

@Setter
@Getter
@Accessors(chain = true)
public class GetOfferListingService {

    int limit;
    int page;
    String pageToken;
    String regionId;
    Request request;
    String sorting;

    public static GetOfferListingService getOfferListingService() {
        return new GetOfferListingService();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
