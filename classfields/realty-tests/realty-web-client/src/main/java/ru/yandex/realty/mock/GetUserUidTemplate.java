package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class GetUserUidTemplate {

    public static final String GET_USE_UID_TEMPLATE = "mock/getUserUidTemplate.json";

    private JsonObject template;

    private GetUserUidTemplate(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static GetUserUidTemplate getUserUidTemplate() {
        return new GetUserUidTemplate(GET_USE_UID_TEMPLATE);
    }

    public GetUserUidTemplate setId(String id) {
        template.getAsJsonObject("response").addProperty("id", id);
        return this;
    }

    public GetUserUidTemplate setMosRuStatus(String status) {
        template.getAsJsonObject("response").getAsJsonObject("trustedUserInfo").addProperty("mosRuTrustedStatus",
                status);
        return this;
    }

    public GetUserUidTemplate setExtendedUserType(String type) {
        template.getAsJsonObject("response").getAsJsonObject("trustedUserInfo").addProperty("extendedUserType", type);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
