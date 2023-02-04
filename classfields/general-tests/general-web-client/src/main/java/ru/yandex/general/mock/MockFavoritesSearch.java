package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.consts.FavoritesNotificationTypes.FavoriteSearchNotifications;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockFavoritesSearch {

    public static final String BASIC_SEARCH = "mock/basicFavoriteSearch.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject savedSearch;

    private MockFavoritesSearch(String pathToSavedSearch) {
        this.savedSearch = new Gson().fromJson(getResourceAsString(pathToSavedSearch), JsonObject.class);
    }

    public static MockFavoritesSearch mockBasicSearch() {
        return new MockFavoritesSearch(BASIC_SEARCH);
    }

    public MockFavoritesSearch addNotification(List<FavoriteSearchNotifications> notifications) {
        notifications.forEach(notification ->
                savedSearch.getAsJsonObject("notificationSettings").getAsJsonArray("notificationChannels")
                        .add(notification.getNotificationType()));
        return this;
    }

    @Step("Добавляем title = «{title}»")
    public MockFavoritesSearch setTitle(String title) {
        savedSearch.addProperty("title", title);
        return this;
    }

    @Step("Добавляем subTitle = «{subtitle}»")
    public MockFavoritesSearch setSubtitle(String subtitle) {
        savedSearch.addProperty("subtitle", subtitle);
        return this;
    }

    @Step("Добавляем searchLink.url = «{link}»")
    public MockFavoritesSearch setSearchLink(String link) {
        JsonObject searchLink = new JsonObject();
        searchLink.addProperty("url", link);
        savedSearch.add("searchLink", searchLink);
        return this;
    }

    @Step("Удаляем фотографии из сохраненного оффера")
    public MockFavoritesSearch removePhotos() {
        JsonArray photos = new JsonArray();
        savedSearch.add("photos", photos);
        return this;
    }

}
