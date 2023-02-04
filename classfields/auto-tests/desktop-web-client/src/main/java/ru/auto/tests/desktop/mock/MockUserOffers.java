package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;

public class MockUserOffers {

    private static final String USER_OFFERS_TEMPLATE = "mocksConfigurable/user/UserOffersTemplate.json";
    private static final String OFFERS_EXAMPLE = "mocksConfigurable/user/OffersExample.json";
    private static final String RESELLER_OFFERS_EXAMPLE = "mocksConfigurable/user/ResellerOffersExample.json";

    public static final String USED = "used";
    public static final String USER_ID = "oDJWm9uHUC4";

    private static final String PAGINATION = "pagination";
    private static final String OFFERS = "offers";

    @Getter
    @Setter
    private JsonObject body;

    @Getter
    private List<MockUserOffer> offers;

    private MockUserOffers(String pathToTemplate) {
        offers = new ArrayList<>();
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserOffers userOffersResponse() {
        return new MockUserOffers(USER_OFFERS_TEMPLATE);
    }

    public static MockUserOffers offersExample() {
        return new MockUserOffers(OFFERS_EXAMPLE);
    }

    public static MockUserOffers resellerOffersExample() {
        return new MockUserOffers(RESELLER_OFFERS_EXAMPLE);
    }

    public MockUserOffers setOffers(MockUserOffer... userOffers) {
        offers.addAll(Arrays.asList(userOffers));
        return this;
    }

    public MockUserOffers setOffers(int count) {
        for (int i = 0; i < count; i++) {
            offers.add(car().setId(getRandomId()));
        }
        return this;
    }

    public MockUserOffers setFiltersStatus(String... statusList) {
        JsonObject filters = new JsonObject();
        JsonArray status = new JsonArray();

        stream(statusList).forEach(status::add);
        filters.add("status", status);

        body.add("filters", filters);
        return this;
    }

    public MockUserOffers setPageCount(int pageCount) {
        body.getAsJsonObject(PAGINATION).addProperty("total_page_count", pageCount);
        return this;
    }

    public MockUserOffers setPage(int page) {
        body.getAsJsonObject(PAGINATION).addProperty("page", page);
        return this;
    }

    public MockUserOffers setIdFirstOffer(String id) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().addProperty("id", id);
        return this;
    }

    public MockUserOffers setSorting(String name, String desc) {
        JsonObject sorting = new JsonObject();

        sorting.addProperty("name", name);
        sorting.addProperty("desc", desc);

        body.add("sorting", sorting);
        return this;
    }

    public JsonObject build() {
        if (offers.size() > 0) {
            JsonArray offersList = new JsonArray();
            offers.forEach(report -> offersList.add(report.getBody()));

            body.add(OFFERS, offersList);
        }

        return body;
    }

}
