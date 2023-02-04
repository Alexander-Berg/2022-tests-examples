package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.consts.FavoritesNotificationTypes.FavoriteSearchNotifications;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockFavoritesSeller {

    public static final String BASIC_SELLER = "mock/basicFavoriteSeller.json";
    private static final String AVATAR = "mock/avatar.json";

    private static final String SELLER = "seller";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject savedSeller;

    private MockFavoritesSeller(String pathToSavedSearch) {
        this.savedSeller = new Gson().fromJson(getResourceAsString(pathToSavedSearch), JsonObject.class);
    }

    public static MockFavoritesSeller mockBasicSeller() {
        return new MockFavoritesSeller(BASIC_SELLER);
    }

    public MockFavoritesSeller addNotification(List<FavoriteSearchNotifications> notifications) {
        notifications.forEach(notification ->
                savedSeller.getAsJsonObject("notificationSettings").getAsJsonArray("notificationChannels")
                        .add(notification.getNotificationType()));
        return this;
    }

    @Step("Добавляем seller.name = «{name}»")
    public MockFavoritesSeller setSellerName(String name) {
        savedSeller.getAsJsonObject(SELLER).addProperty("name", name);
        return this;
    }

    @Step("Добавляем offersCount = «{offersCount}»")
    public MockFavoritesSeller setOffersCount(int offersCount) {
        savedSeller.addProperty("offersCount", offersCount);
        return this;
    }

    @Step("Добавляем seller.publicProfileLink.url = «{url}»")
    public MockFavoritesSeller setPublicProfileUrl(String url) {
        savedSeller.getAsJsonObject(SELLER).getAsJsonObject("publicProfileLink").addProperty("url", url);
        return this;
    }

    @Step("Добавляем аватар")
    public MockFavoritesSeller setAvatar() {
        JsonObject avatar = new Gson().fromJson(getResourceAsString(AVATAR), JsonObject.class);
        savedSeller.getAsJsonObject(SELLER).add("avatar", avatar);
        return this;
    }

}
