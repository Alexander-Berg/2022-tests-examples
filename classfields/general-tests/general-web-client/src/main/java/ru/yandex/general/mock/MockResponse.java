package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.graphql.Response;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingExample;
import static ru.yandex.general.mock.MockCard.cardTemplate;
import static ru.yandex.general.mock.MockCategories.categoriesTemplate;
import static ru.yandex.general.mock.MockCategories.recommendedRootCategoriesTemplate;
import static ru.yandex.general.mock.MockCategory.categoryTemplate;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockDistricts.districtsTemplate;
import static ru.yandex.general.mock.MockFavorites.favoritesExample;
import static ru.yandex.general.mock.MockMetro.metroTemplate;
import static ru.yandex.general.mock.MockRegions.regionsTemplate;
import static ru.yandex.general.mock.MockSearch.categoryListingExample;

public class MockResponse {

    private static final String RESPONSE_TEMPLATE = "mock/responseTemplate.json";

    private static final String DATA = "data";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private Response response;

    private MockResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockResponse mockResponse() {
        return new MockResponse(RESPONSE_TEMPLATE);
    }

    @Step("Добавляем мок для «setCabinetListing»")
    public MockResponse setCabinetListing(String cabinetListing) {
        template.getAsJsonObject(DATA)
                .add("cabinetListing", new GsonBuilder().create().fromJson(cabinetListing, JsonObject.class));
        return this;
    }

    public MockResponse setCabinetListingExample() {
        setCabinetListing(cabinetListingExample().build());
        return this;
    }

    @Step("Добавляем мок для «setCurrentUser»")
    public MockResponse setCurrentUser(String currentUser) {
        template.getAsJsonObject(DATA)
                .add("currentUser", new GsonBuilder().create().fromJson(currentUser, JsonObject.class));
        return this;
    }

    public MockResponse setCurrentUserExample() {
        setCurrentUser(currentUserExample().build());
        return this;
    }

    @Step("Добавляем мок для «GetCategories»")
    public MockResponse setCategories(String categories) {
        template.getAsJsonObject(DATA)
                .add("categories", new GsonBuilder().create().fromJson(categories, JsonArray.class));
        return this;
    }

    @Step("Добавляем мок для «GetRecommendedCategories»")
    public MockResponse setRecommendedRootCategories(String categories) {
        template.getAsJsonObject(DATA)
                .add("recommendedRootCategories", new GsonBuilder().create().fromJson(categories, JsonArray.class));
        return this;
    }

    public MockResponse setCategoriesTemplate() {
        setCategories(categoriesTemplate().build());
        setRecommendedRootCategories(recommendedRootCategoriesTemplate().build());
        return this;
    }

    @Step("Добавляем мок для «setRegions»")
    public MockResponse setRegions(String regions) {
        template.getAsJsonObject(DATA)
                .add("regions", new GsonBuilder().create().fromJson(regions, JsonObject.class));
        return this;
    }

    public MockResponse setRegionsTemplate() {
        setRegions(regionsTemplate().build());
        return this;
    }

    @Step("Добавляем мок для «setSearch»")
    public MockResponse setSearch(String search) {
        template.getAsJsonObject(DATA)
                .add("search", new GsonBuilder().create().fromJson(search, JsonObject.class));
        return this;
    }

    public MockResponse setCategoryListingExample() {
        setSearch(categoryListingExample().build());
        return this;
    }

    @Step("Добавляем мок для «setCard»")
    public MockResponse setCard(String card) {
        template.getAsJsonObject(DATA)
                .add("card", new GsonBuilder().create().fromJson(card, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «Toponyms»")
    public MockResponse setToponyms(String toponyms) {
        template.getAsJsonObject(DATA)
                .add("toponyms", new GsonBuilder().create().fromJson(toponyms, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для phone = «{phone}»")
    public MockResponse setPhone(String phone) {
        JsonObject card = new JsonObject();
        JsonObject contacts = new JsonObject();
        contacts.addProperty("phone", phone);
        card.add("contacts", contacts);

        template.getAsJsonObject(DATA)
                .add("card", card);
        return this;
    }

    @Step("Добавляем мок для «feed»")
    public MockResponse setFeed(String feed) {
        template.getAsJsonObject(DATA)
                .add("feed", new GsonBuilder().create().fromJson(feed, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «taskInfo»")
    public MockResponse setTaskInfo(String taskInfo) {
        template.getAsJsonObject(DATA)
                .add("taskInfo", new GsonBuilder().create().fromJson(taskInfo, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «tasks»")
    public MockResponse setTasks(String tasks) {
        template.getAsJsonObject(DATA)
                .add("tasks", new GsonBuilder().create().fromJson(tasks, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «feedErrors»")
    public MockResponse setFeedErrors(String feedErrors) {
        template.getAsJsonObject(DATA)
                .add("feedErrors", new GsonBuilder().create().fromJson(feedErrors, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «getOfferPhone» с телефоном «{phone}»")
    public MockResponse setOfferPhone(String phone) {
        JsonObject contacts = new JsonObject();
        contacts.addProperty("phone", phone);
        JsonObject card = new JsonObject();
        card.add("contacts", contacts);
        template.getAsJsonObject(DATA).add("card", card);
        return this;
    }

    @Step("Добавляем мок для «getChat»")
    public MockResponse setChat(String chat) {
        template.getAsJsonObject(DATA).add("getChat", new GsonBuilder().create().fromJson(chat, JsonObject.class));
        return this;
    }

    public MockResponse setCardTemplate() {
        setCard(cardTemplate().build());
        return this;
    }

    @Step("Добавляем мок для «setFavorites»")
    public MockResponse setFavorites(String favorites) {
        template.getAsJsonObject(DATA)
                .add("favorites", new GsonBuilder().create().fromJson(favorites, JsonObject.class));
        return this;
    }

    public MockResponse setFavoritesExample() {
        setFavorites(favoritesExample().build());
        return this;
    }

    @Step("Добавляем мок для «metro»")
    public MockResponse setMetro(String metro) {
        template.getAsJsonObject(DATA)
                .add("metro", new GsonBuilder().create().fromJson(metro, JsonObject.class));
        return this;
    }

    public MockResponse setMetroTemplate() {
        setMetro(metroTemplate().build());
        return this;
    }

    @Step("Добавляем мок для «districts»")
    public MockResponse setDistricts(String districts) {
        template.getAsJsonObject(DATA)
                .add("districts", new GsonBuilder().create().fromJson(districts, JsonObject.class));
        return this;
    }

    public MockResponse setDistrictsTemplate() {
        setDistricts(districtsTemplate().build());
        return this;
    }

    @Step("Добавляем мок для «currentDraft»")
    public MockResponse setCurrentDraft(String currentDraft) {
        template.getAsJsonObject(DATA)
                .add("currentDraft", new GsonBuilder().create().fromJson(currentDraft, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «category»")
    public MockResponse setCategory(String category) {
        template.getAsJsonObject(DATA)
                .add("category", new GsonBuilder().create().fromJson(category, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «homePage»")
    public MockResponse setHomepage(String homepage) {
        template.getAsJsonObject(DATA).add("homepage", new GsonBuilder().create().fromJson(homepage, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «publicProfile»")
    public MockResponse setPublicProfile(String publicProfile) {
        template.getAsJsonObject(DATA).add("publicProfile",
                new GsonBuilder().create().fromJson(publicProfile, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «userStatistics»")
    public MockResponse setUserStatistics(String userStatistics) {
        template.getAsJsonObject(DATA).add("getUserStatistics",
                new GsonBuilder().create().fromJson(userStatistics, JsonObject.class));
        return this;
    }

    @Step("Добавляем мок для «notificationsSettings»")
    public MockResponse setNotificationSettings(String notificationsSettings) {
        template.getAsJsonObject(DATA)
                .add("notificationsSettings", new GsonBuilder().create()
                        .fromJson(notificationsSettings, JsonObject.class));
        return this;
    }

    public MockResponse setCategoryTemplate() {
        setCategory(categoryTemplate().build());
        return this;
    }

    public String build() {
        return template.toString();
    }

}
