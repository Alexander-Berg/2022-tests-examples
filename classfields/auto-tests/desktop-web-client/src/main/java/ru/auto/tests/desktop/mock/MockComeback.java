package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.mock.beans.comeback.Comeback;
import ru.auto.tests.desktop.mock.beans.comeback.Filter;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.comeback.Comeback.comeback;
import static ru.auto.tests.desktop.mock.beans.comeback.Filter.filter;
import static ru.auto.tests.desktop.mock.beans.comeback.Pagination.pagination;

public class MockComeback {

    public static final String COMEBACK_EXAMPLE = "mocksConfigurable/comeback/ComebackExample.json";
    public static final String COMEBACK_EXAMPLE_ONE_OFFER = "mocksConfigurable/comeback/ComebackExampleOneOffer.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockComeback(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockComeback mockComeback(String pathToTemplate) {
        return new MockComeback(pathToTemplate);
    }

    public static MockComeback comebackExample() {
        return mockComeback(COMEBACK_EXAMPLE);
    }

    public static MockComeback comebackExampleOneOffer() {
        return mockComeback(COMEBACK_EXAMPLE_ONE_OFFER);
    }

    public MockComeback setStatusSuccessResponse() {
        body.addProperty("status", "SUCCESS");
        return this;
    }

    @Step("Добавляем сортировку = «{sorting}» в ответ мока comeback")
    public MockComeback setSorting(String sorting) {
        body.getAsJsonObject("request").addProperty("sorting", sorting);
        return this;
    }

    @Step("Убираем офферы из ответа мока comeback")
    public MockComeback setNoOffers() {
        body.remove("comebacks");
        body.getAsJsonObject("pagination").addProperty("total_offers_count", 0);
        return this;
    }

    public static Comeback comebackRequest() {
        return comeback()
                .setFilter(getFilter())
                .setPagination(pagination().setPage(1).setPageSize(10))
                .setSorting("CREATION_DATE");
    }

    public static Filter getFilter() {
        return filter().setCreationDateFrom("\\d+")
                .setCreationDateTo("\\d+")
                .setRid(asList(213))
                .setGeoRadius(200);
    }

}
