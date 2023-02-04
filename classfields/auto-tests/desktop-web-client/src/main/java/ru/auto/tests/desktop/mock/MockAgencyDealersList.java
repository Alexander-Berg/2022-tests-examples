package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockAgencyDealersList {

    public static final String AGENCY_DEALERS_LIST_EXAMPLE = "mocksConfigurable/cabinet/AgencyDealersListExample.json";
    public static final String AGENCY_DEALERS_LIST_REQUEST = "mocksConfigurable/cabinet/AgencyDealersListRequest.json";
    public static final String AGENCY_DEALERS_CLIENT = "mocksConfigurable/cabinet/AgencyDealersClient.json";
    private static final String PAGINATION = "pagination";
    private static final String CLIENTS = "clients";
    private static final String FILTER = "filter";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockAgencyDealersList(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockAgencyDealersList agencyDealersListRequest() {
        return new MockAgencyDealersList(AGENCY_DEALERS_LIST_REQUEST);
    }

    public static MockAgencyDealersList agencyDealersListResponseBody() {
        return new MockAgencyDealersList(AGENCY_DEALERS_LIST_EXAMPLE);
    }

    @Step("Добавляем страницу page = «{pageId}»")
    public MockAgencyDealersList setPage(int pageId) {
        body.getAsJsonObject(PAGINATION).addProperty("page", pageId);
        return this;
    }

    @Step("Добавляем страницу page = «{pageId}» для отображения в пагинаторе")
    public MockAgencyDealersList setPageResponse(int pageId) {
        body.getAsJsonObject(PAGINATION).addProperty("page_num", pageId);
        return this;
    }

    @Step("Меняем статус на «{status}»")
    public MockAgencyDealersList changeStatus(String status) {
        body.getAsJsonObject(FILTER).addProperty("preset", status);
        return this;
    }

    @Step("Меняем статус на «{status}»")
    public MockAgencyDealersList changeStatusResponse(int offer, String status) {
        body.getAsJsonArray(CLIENTS).get(offer).getAsJsonObject().addProperty("status", status);
        return this;
    }

    @Step("Добавляем в запрос клиента «{origin}»")
    public MockAgencyDealersList setClientId(String origin) {
        body.getAsJsonObject(FILTER).addProperty("origin", origin);
        return this;
    }

    @Step("Добавляем в ответ клиента «{origin}» в оффер «{offer}»")
    public MockAgencyDealersList setClientIdResponse(int offer, String origin) {
        body.getAsJsonArray(CLIENTS).get(offer).getAsJsonObject().addProperty("origin", origin);
        return this;
    }

    @Step("Заменяем список клиентов на одного клиента «{origin}»")
    public MockAgencyDealersList changeClientsListToClientId(String origin) {
        body.remove(CLIENTS);
        setClient();
        setClientIdResponse(0, origin);
        return this;
    }

    public MockAgencyDealersList setClient() {
        JsonArray clients = new GsonBuilder().create().fromJson(
                getResourceAsString(AGENCY_DEALERS_CLIENT), JsonArray.class);
        body.add(CLIENTS, clients);
        return this;
    }

}
