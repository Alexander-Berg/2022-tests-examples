package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockPublicProfile {

    private static final String PUBLIC_PROFILE_TEMPLATE = "mock/publicProfile/publicProfileTemplate.json";

    public static final String AVATAR_URL = "https://avatars.mdst.yandex.net/get-yapic/1450/EBKueFQyfTAqmGw3d0uSxeUJ2E-1/islands-retina-50";
    public static final String AVATAR_DUMMY_IMG_URL = "https://avatars.mds.yandex.net/get-yapic/0/0-0/islands-retina-50";

    private static final String LISTING = "listing";
    private static final String SNIPPETS = "snippets";
    private static final String SELLER = "seller";
    private static final String AVAILABLE_PRESETS = "availablePresets";
    private static final String COUNT = "count";
    private static final String AVATAR = "avatar";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockPublicProfileSnippet> snippets;

    @Getter
    @Setter
    private JsonObject profile;

    private MockPublicProfile(String pathToTemplate) {
        this.profile = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockPublicProfile profileResponse() {
        return new MockPublicProfile(PUBLIC_PROFILE_TEMPLATE);
    }

    @Step("Добавляем имя продавца = «{name}»")
    public MockPublicProfile setSellerName(String name) {
        profile.getAsJsonObject(SELLER).addProperty("name", name);
        return this;
    }

    public MockPublicProfile setUserBadgeScore(String score) {
        profile.getAsJsonObject(SELLER).getAsJsonObject("userBadge").addProperty("score", score);
        return this;
    }

    @Step("Добавляем «{count}» активных объявлений")
    public MockPublicProfile setActiveCount(int count) {
        JsonObject preset = new JsonObject();
        preset.addProperty("preset", "Active");
        preset.addProperty(COUNT, count);
        profile.getAsJsonObject(LISTING).getAsJsonArray(AVAILABLE_PRESETS).add(preset);
        return this;
    }

    @Step("Добавляем «{count}» истекших объявлений")
    public MockPublicProfile setExpiredCount(int count) {
        JsonObject preset = new JsonObject();
        preset.addProperty("preset", "ExpiredOrSold");
        preset.addProperty(COUNT, count);
        profile.getAsJsonObject(LISTING).getAsJsonArray(AVAILABLE_PRESETS).add(preset);
        return this;
    }

    @Step("Добавляем аватар продавца")
    public MockPublicProfile setAvatar() {
        JsonObject avatar = new JsonObject();
        avatar.addProperty("size_100x100", AVATAR_URL);
        profile.getAsJsonObject(SELLER).add("avatar", avatar);
        return this;
    }

    @Step("Убираем аватар продавца")
    public MockPublicProfile removeAvatar() {
        profile.getAsJsonObject(SELLER).add(AVATAR, null);
        return this;
    }

    @Step("Добавляем кол-во подписчиков = «{count}»")
    public MockPublicProfile setFollowers(int count) {
        profile.addProperty("followers", count);
        return this;
    }

    @Step("Добавляем кол-во подписок = «{count}»")
    public MockPublicProfile setFollowing(int count) {
        profile.addProperty("following", count);
        return this;
    }

    @Step("Добавляем publicId = «{publicId}»")
    public MockPublicProfile setPublicId(String publicId) {
        profile.getAsJsonObject(SELLER).addProperty("publicId", publicId);
        return this;
    }

    public String build() {
        if (snippets != null) {
            JsonArray snippetsArray = new JsonArray();
            snippets.forEach(o -> snippetsArray.add(o.getSnippet()));
            profile.getAsJsonObject(LISTING).addProperty("totalCount", snippetsArray.size());
            profile.getAsJsonObject(LISTING).add(SNIPPETS, snippetsArray);
        }

        return profile.toString();
    }

}
