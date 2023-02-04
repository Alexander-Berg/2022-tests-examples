package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerPriceListing {

    @Getter
    @Setter
    @Accessors(chain = true)
    private JsonObject body;

    @Getter
    private List<MockDealerPriceOffer> offers;

    private MockDealerPriceListing() {
        offers = new ArrayList<>();
        body = new JsonObject();
    }

    public static MockDealerPriceListing dealerPriceListing() {
        return new MockDealerPriceListing();
    }

    public static MockDealerPriceListing dealerPriceListing(String pathToTemplate) {
        return new MockDealerPriceListing().setBody(
                new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class));
    }

    public MockDealerPriceListing setOffers(MockDealerPriceOffer... priceOffers) {
        offers.addAll(Arrays.asList(priceOffers));
        return this;
    }

    public JsonObject build() {
        if (offers.size() > 0) {
            JsonArray priceOffers = new JsonArray();
            offers.forEach(priceGroup -> priceOffers.add(priceGroup.getBody()));

            JsonObject pagination = new JsonObject();
            pagination.addProperty("page", 1);
            pagination.addProperty("page_size", 10);
            pagination.addProperty("total_offers_count", priceOffers.size());
            pagination.addProperty("total_page_count", 1);

            body.add("offers", priceOffers);
            body.add("pagination", pagination);
        }

        return body;
    }

}
