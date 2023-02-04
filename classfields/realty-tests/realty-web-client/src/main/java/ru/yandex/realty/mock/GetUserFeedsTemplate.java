package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class GetUserFeedsTemplate {

    public static final String GET_USER_FEEDS_TEMPLATE = "mock/getUserFeedsTemplate.json";

    private JsonObject template;

    private GetUserFeedsTemplate(String pathToTemplate) {
        template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static GetUserFeedsTemplate getUserFeedsTemplate() {
        return new GetUserFeedsTemplate(GET_USER_FEEDS_TEMPLATE);
    }

    public GetUserFeedsTemplate setPartnerId(String partnerId) {
        template.getAsJsonArray("response").get(0).getAsJsonObject().addProperty("partnerId", partnerId);
        return this;
    }

    public String getPartnerId() {
        return template.getAsJsonArray("response").get(0).getAsJsonObject().getAsJsonPrimitive("partnerId")
                .getAsString();
    }

    public JsonObject getJson() {
        return template;
    }

    public String build() {
        return template.toString();
    }
}
