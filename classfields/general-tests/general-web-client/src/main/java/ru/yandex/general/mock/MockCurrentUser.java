package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.consts.UserStatus.UserBanDescriptions;

import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.consts.UserStatus.BANNED;

public class MockCurrentUser {

    private static final String CURRENT_USER_EXAMPLE = "mock/currentUserExample.json";
    private static final String CURRENT_USER_TEMPLATE = "mock/currentUserTemplate.json";
    private static final String AVATAR = "mock/avatar.json";

    private static final String MODERATION = "moderation";
    private static final String OFFERS_PUBLISH = "offersPublish";


    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject currentUser;

    private MockCurrentUser(String pathToTemplate) {
        this.currentUser = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCurrentUser currentUserExample() {
        return new MockCurrentUser(CURRENT_USER_EXAMPLE);
    }

    public static MockCurrentUser currentUserTemplate() {
        return new MockCurrentUser(CURRENT_USER_TEMPLATE);
    }

    @Step("Добавляем moderation.offersPublish.status = «{status}»")
    public MockCurrentUser setOffersPublishStatus(String status) {
        currentUser.getAsJsonObject(MODERATION).getAsJsonObject(OFFERS_PUBLISH).addProperty("status", status);
        return this;
    }

    @Step("Добавляем moderation.offersPublish.status = «{status}»")
    public MockCurrentUser setModerationChatStatus(String status) {
        currentUser.getAsJsonObject(MODERATION).getAsJsonObject("chats").addProperty("status", status);
        return this;
    }

    @Step("Добавляем name = «{name}»")
    public MockCurrentUser setUserName(String name) {
        currentUser.addProperty("name", name);
        return this;
    }

    @Step("Добавляем email = «{email}»")
    public MockCurrentUser setUserEmail(String email) {
        currentUser.addProperty("email", email);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockCurrentUser setUserId(String id) {
        currentUser.addProperty("id", id);
        return this;
    }

    @Step("Добавляем аватар")
    public MockCurrentUser setAvatar() {
        JsonObject avatar = new Gson().fromJson(getResourceAsString(AVATAR), JsonObject.class);
        currentUser.add("avatar", avatar);
        return this;
    }

    @Step("Удаляем аватар")
    public MockCurrentUser removeAvatar() {
        currentUser.add("avatar", null);
        return this;
    }

    @Step("Добавляем телефоны")
    public MockCurrentUser addPhones(String... phones) {
        JsonArray phonesList = new JsonArray();
        stream(phones).forEach(phone -> phonesList.add(phone));
        currentUser.add("phones", phonesList);
        return this;
    }

    @Step("Добавляем адреса")
    public MockCurrentUser addAddresses(Address... addresses) {
        JsonArray addressesList = new JsonArray();
        stream(addresses).forEach(address -> addressesList.add(new Gson().toJsonTree(address).getAsJsonObject()));
        currentUser.add("addresses", addressesList);
        return this;
    }

    @Step("Добавляем блок телефона YML = «{ymlEnabled}»")
    public MockCurrentUser ymlEnabled(boolean ymlEnabled) {
        currentUser.getAsJsonObject("ymlPhone").addProperty("enabled", ymlEnabled);
        return this;
    }

    @Step("Добавляем телефон YML = «{phone}»")
    public MockCurrentUser setYmlPhone(String phone) {
        currentUser.getAsJsonObject("ymlPhone").addProperty("phone", phone);
        return this;
    }

    @Step("Добавляем publicId = «{publicId}»")
    public MockCurrentUser setPublicId(String publicId) {
        currentUser.addProperty("publicId", publicId);
        return this;
    }

    public MockCurrentUser setUserBannedWithDescription(UserBanDescriptions... banDescriptions) {
        JsonArray banDescriptionsArray = new JsonArray();

        for (UserBanDescriptions banDescription : banDescriptions) {
            JsonObject description = new JsonObject();

            description.addProperty("title", banDescription.getTitle());
            description.addProperty("textHtml", banDescription.getTextHtml());

            banDescriptionsArray.add(description);
        }
        setOffersPublishStatus(BANNED);
        currentUser.getAsJsonObject(MODERATION).getAsJsonObject(OFFERS_PUBLISH).add("banDescriptions", banDescriptionsArray);

        return this;
    }

    public String build() {
        return currentUser.toString();
    }

}
