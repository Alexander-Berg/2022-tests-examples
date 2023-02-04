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
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.utils.Utils.getRandomOfferId;

public class MockFavorites {

    private static final String FAVORITES_EXAMPLE = "mock/favorites.json";
    private static final String FAVORITES_TEMPLATE = "mock/favoritesTemplate.json";

    private static final String OFFERS = "offers";
    private static final String SEARCHES = "searches";
    private static final String SELLERS = "sellers";
    private static final String LISTING = "listing";
    private static final String SNIPPETS = "snippets";
    private static final String COUNT = "count";

    private String pathToTemplate;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockFavoritesSnippet> offers;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockFavoritesSearch> searches;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockFavoritesSeller> sellers;

    private MockFavorites(String pathToTemplate) {
        this.pathToTemplate = pathToTemplate;
    }

    public static MockFavorites favoritesResponse() {
        return new MockFavorites(FAVORITES_TEMPLATE);
    }

    public static MockFavorites favoritesExample() {
        return new MockFavorites(FAVORITES_EXAMPLE);
    }

    public static MockFavorites mockFavorites(String pathToMockFavorites) {
        return new MockFavorites(pathToMockFavorites);
    }

    @Step("Добавляем «{count}» офферов")
    public MockFavorites addOffers(int count) {
        offers = new JSONArray();
        for (int i = 0; i < count; i++) {
            offers.add(mockSnippet(BASIC_SNIPPET).getMockSnippet().setId(getRandomOfferId()));
        }
        return this;
    }

    @Step("Добавляем «{count}» сохраненных поисков")
    public MockFavorites addSearches(int count) {
        searches = new JSONArray();
        for (int i = 0; i < count; i++) {
            searches.add(MockFavoritesSearch.mockBasicSearch());
        }
        return this;
    }

    public String build() {
        JsonObject template = new Gson().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);

        if (offers != null) {
            JsonArray offersArray = new JsonArray();
            offers.forEach(o -> offersArray.add(o.getSnippet()));
            template.getAsJsonObject(OFFERS).addProperty(COUNT, offersArray.size());
            template.getAsJsonObject(OFFERS).getAsJsonObject(LISTING).add(SNIPPETS, offersArray);
        }

        if (searches != null) {
            JsonArray searchesArray = new JsonArray();
            searches.forEach(o -> searchesArray.add(o.getSavedSearch()));
            template.getAsJsonObject(SEARCHES).addProperty(COUNT, searchesArray.size());
            template.getAsJsonObject(SEARCHES).getAsJsonObject(LISTING).addProperty("totalCount", searchesArray.size());
            template.getAsJsonObject(SEARCHES).getAsJsonObject(LISTING).add(SNIPPETS, searchesArray);
        }

        if (sellers != null) {
            JsonArray sellersArray = new JsonArray();
            sellers.forEach(o -> sellersArray.add(o.getSavedSeller()));
            template.getAsJsonObject(SELLERS).addProperty(COUNT, sellersArray.size());
            template.getAsJsonObject(SELLERS).getAsJsonObject(LISTING).add(SNIPPETS, sellersArray);
        }

        return template.toString();
    }

}
