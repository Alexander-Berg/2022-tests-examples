package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.simple.JSONArray;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.utils.Utils.getRandomOfferId;

public class MockHomepage {

    private static final String HOMEPAGE_TEMPLATE = "mock/homepageTemplate.json";

    private static final String LISTING = "listing";

    private String pathToTemplate;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockListingSnippet> offers;

    private MockHomepage(String pathToTemplate) {
        this.pathToTemplate = pathToTemplate;
    }

    public static MockHomepage homepageResponse() {
        return new MockHomepage(HOMEPAGE_TEMPLATE);
    }

    public static MockHomepage mockHomepage(String pathToMockHomepage) {
        return new MockHomepage(pathToMockHomepage);
    }

    @Step("Добавляем «{count}» офферов")
    public MockHomepage addOffers(int count) {
        offers = new JSONArray();
        for (int i = 0; i < count; i++) {
            offers.add(mockSnippet(BASIC_SNIPPET).getMockSnippet().setId(getRandomOfferId()));
        }
        return this;
    }

    public String build() {
        JsonObject template = new Gson().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
        JsonArray offersArray = new JsonArray();

        if (offers != null) {
            offers.forEach(o -> offersArray.add(o.getSnippet()));
            template.getAsJsonObject(LISTING).addProperty("totalCount", offersArray.size());
            template.getAsJsonObject(LISTING).add("snippets", offersArray);
        }

        return template.toString();
    }
}
