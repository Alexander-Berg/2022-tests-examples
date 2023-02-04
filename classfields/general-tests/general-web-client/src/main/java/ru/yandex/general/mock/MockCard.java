package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.simple.JSONArray;
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.beans.card.Attribute;
import ru.yandex.general.beans.card.Statistics;
import ru.yandex.general.beans.card.StatisticsGraphItem;
import ru.yandex.general.consts.BaseConstants;
import ru.yandex.general.consts.CardStatus;

import java.util.Calendar;
import java.util.List;

import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TEXT;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TITLE;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getISOFormatedDate;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.general.utils.Utils.getRandomOfferId;
import static ru.yandex.general.utils.Utils.getStatisticsGraph;

public class MockCard {

    public static final String BASIC_CARD = "mock/card/basicCard.json";
    public static final String MAX_MEDIA_CARD = "mock/card/maxMediaCard.json";
    public static final String REZUME_CARD = "mock/card/rezumeCard.json";
    public static final String VACANCY_CARD = "mock/card/vacancyCard.json";
    private static final String PHOTO_ITEMS = "mock/card/photoItems.json";
    private static final String AVATAR = "mock/avatar.json";
    private static final String SIMILAR_OFFERS_TEMPLATE = "mock/similarOffersTemplate.json";

    public static final String PHOTO_1 = "mock/card/photos/photo_1.json";
    public static final String PHOTO_2 = "mock/card/photos/photo_2.json";
    public static final String PHOTO_3 = "mock/card/photos/photo_3.json";

    private static final String PRICE = "price";
    private static final String CURRENT_PRICE = "currentPrice";
    private static final String STATUS = "status";
    private static final String REASON = "reason";
    private static final String TITLE = "title";
    private static final String TYPE = "type";
    private static final String TYPENAME = "__typename";
    private static final String PRICE_RUR = "priceRur";
    private static final String ID = "id";
    private static final String PHOTOS = "photos";
    private static final String CATEGORY = "category";
    private static final String SELLER = "seller";
    private static final String REMOVE = "remove";
    private static final String EDIT = "edit";
    private static final String ACTIVATE = "activate";
    private static final String HIDE = "hide";
    public static final String CHAT = "Chat";
    public static final String PHONE_CALL = "PhoneCall";
    public static final String ANY = "Any";

    private String createDateTime;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject card;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockListingSnippet> similarOffers;

    private MockCard(String pathToTemplate) {
        this.card = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCard mockCard(String pathToMockCard) {
        return new MockCard(pathToMockCard);
    }

    public static MockCard cardTemplate() {
        return new MockCard(BASIC_CARD);
    }

    @Step("Удаляем фотографии из карточки")
    public MockCard removePhotos() {
        JsonArray photos = new JsonArray();
        card.add(PHOTOS, photos);
        return this;
    }

    @Step("Устанавливаем preferContactWay = «{contactWay}»")
    public MockCard setPreferContactWay(String contactWay) {
        card.getAsJsonObject("contacts").addProperty("preferContactWay", contactWay);
        return this;
    }

    @Step("Добавляем price = «{price}»")
    public MockCard setPrice(long price) {
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(PRICE_RUR, price);
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "InCurrency");
        return this;
    }

    @Step("Добавляем зарплату = «{sallaryPrice}»")
    public MockCard setSallaryPrice(String sallaryPrice) {
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Salary");
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty("salaryRur", sallaryPrice);
        return this;
    }

    @Step("Устанавливаем стоимость - даром")
    public MockCard setFreePrice() {
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Free");
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем стоимость - цена не указана")
    public MockCard setUnsetPrice() {
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Unset");
        card.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем status = «{status}»")
    public MockCard setStatus(String status) {
        card.getAsJsonObject(STATUS).addProperty(TYPE, status);
        return this;
    }

    @Step("Добавляем причины бана")
    public MockCard setBanReasons(CardStatus.OfferBanReasons... banReasons) {
        JsonObject description = new JsonObject();
        JsonArray reasons = new JsonArray();

        stream(banReasons).forEach(banReason -> {
            JsonObject reason = new JsonObject();
            reason.addProperty("code", banReason.getCode());
            reason.addProperty(TITLE, banReason.getTitle());
            reason.addProperty(REASON, banReason.getReason());
            reasons.add(reason);
        });

        description.add("reasons", reasons);
        card.getAsJsonObject(STATUS).add("description", description);
        return this;
    }

    @Step("Добавляем причину деактивации оффера «{reasonType}»")
    public MockCard setInactiveReason(String reasonType) {
        JsonObject reason = new JsonObject();
        reason.addProperty(TYPE, reasonType);
        card.getAsJsonObject(STATUS).add(REASON, reason);
        return this;
    }

    @Step("Добавляем причину снятия «Вы долго не выходили на связь»")
    public MockCard setCantCallInactiveReason() {
        JsonObject reason = new JsonObject();
        reason.addProperty(TYPE, "ModerationReason");
        reason.addProperty("reasonText", CANT_CALL_REASON_TEXT);
        reason.addProperty(TITLE, CANT_CALL_REASON_TITLE);

        card.getAsJsonObject(STATUS).add(REASON, reason);
        return this;
    }

    @Step("Добавляем isOwner = «{isOwner}»")
    public MockCard setIsOwner(boolean isOwner) {
        card.addProperty("isOwner", isOwner);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockCard setId(String id) {
        card.addProperty(ID, id);
        return this;
    }

    @Step("Добавляем offerOrigin = «{offerOrigin}»")
    public MockCard setOfferOrigin(String offerOrigin) {
        card.addProperty("offerOrigin", offerOrigin);
        return this;
    }

    @Step("Добавляем offerVersion = «{offerVersion}»")
    public MockCard setOfferVersion(String offerVersion) {
        card.addProperty("offerVersion", offerVersion);
        return this;
    }

    @Step("Добавляем categoryId = «{categoryId}»")
    public MockCard setCategoryId(String categoryId) {
        card.getAsJsonObject(CATEGORY).addProperty(ID, categoryId);
        return this;
    }

    @Step("Добавляем «{count}» фото")
    public MockCard addPhoto(int count) {
        JsonArray photos = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonArray photoItemList = new GsonBuilder().create().fromJson(
                    getResourceAsString(PHOTO_ITEMS), JsonArray.class);
            photos.add(photoItemList.get(getRandomIntInRange(0, photoItemList.size() - 1)));
        }
        card.getAsJsonObject().add(PHOTOS, photos);
        return this;
    }

    @Step("Добавляем фото")
    public MockCard addPhoto(String... photoPaths) {
        JsonArray photos = new JsonArray();
        stream(photoPaths).forEach(photoPath -> {
            JsonObject photo = new GsonBuilder().create().fromJson(
                    getResourceAsString(photoPath), JsonObject.class);
            photos.add(photo);
        });
        card.getAsJsonObject().add(PHOTOS, photos);
        return this;
    }

    @Step("Добавляем телефон «{phone}»")
    public MockCard setPhone(String phone) {
        card.getAsJsonObject("contacts").addProperty("phone", phone);
        return this;
    }

    @Step("Добавляем isRedirectPhone = «{isRedirectPhone}»")
    public MockCard setIsRedirectPhone(boolean isRedirectPhone) {
        card.getAsJsonObject("contacts").addProperty("isRedirectPhone", isRedirectPhone);
        return this;
    }

    @Step("Добавляем название «{title}»")
    public MockCard setTitle(String title) {
        card.addProperty(TITLE, title);
        return this;
    }

    @Step("Добавляем category.forAdults = «{forAdults}»")
    public MockCard setCategoryForAdults(boolean forAdults) {
        card.getAsJsonObject(CATEGORY).addProperty("forAdults", forAdults);
        return this;
    }

    @Step("Добавляем seller.userBadge.score = «{score}»")
    public MockCard setUserBadgeScore(String score) {
        card.getAsJsonObject("seller").getAsJsonObject("userBadge").addProperty("score", score);
        return this;
    }

    @Step("Добавляем «statisticsGraph» за последние «{daysCount}» дней")
    public MockCard setStatisticsGraph(int daysCount) {
        card.add("statisticsGraph", getStatisticsGraph(daysCount));
        return this;
    }

    @Step("Добавляем «statisticsGraph» за последние «{daysCount}» дней")
    public MockCard setStatisticsGraph(List<StatisticsGraphItem> statisticsGraphItems) {
        JsonArray records = new JsonArray();
        JsonObject statisticsGraph = new JsonObject();

        statisticsGraphItems.stream().forEach(graphItem -> {
            records.add(new Gson().toJsonTree(graphItem).getAsJsonObject());
        });
        statisticsGraph.add("records", records);

        card.add("statisticsGraph", statisticsGraph);
        return this;
    }

    @Step("Добавляем statistics.today")
    public MockCard setTodayStatistics(Statistics statistics) {
        card.getAsJsonObject("statistics").add("today", new Gson().toJsonTree(statistics).getAsJsonObject());
        return this;
    }

    @Step("Добавляем statistics.total")
    public MockCard setTotalStatistics(Statistics statistics) {
        card.getAsJsonObject("statistics").add("total", new Gson().toJsonTree(statistics).getAsJsonObject());
        return this;
    }

    @Step("Добавляем «createDateTime»")
    public MockCard setCreateDateTime() {
        if (createDateTime != null)
            card.addProperty("createDateTime", createDateTime);
        else
            card.addProperty("createDateTime", getCreateDateTime());
        return this;
    }

    public MockCard setCreateDateTime(String createDateTime) {
        this.createDateTime = createDateTime;
        return this;
    }

    @Step("Добавляем VAS")
    public MockCard setVas() {
        JsonArray purchasableProducts = new JsonArray();
        JsonObject vas = new JsonObject();
        vas.addProperty("productCode", "raise_1");
        vas.addProperty("priceKopecks", "4900");
        vas.addProperty("productType", "FreeRaiseVas");
        purchasableProducts.add(vas);
        card.add("purchasableProducts", purchasableProducts);
        return this;
    }

    @Step("Добавляем ссылку на видео")
    public MockCard setVideoUrl(String videoUrl) {
        JsonObject video = new JsonObject();
        video.addProperty("url", videoUrl);
        card.add("video", video);
        return this;
    }

    @Step("Добавляем имя продавца = «{sellerName}»")
    public MockCard setSellerName(String sellerName) {
        card.getAsJsonObject(SELLER).addProperty("name", sellerName);
        return this;
    }

    @Step("Добавляем кол-во офферов продавца = «{activeOffersCount}»")
    public MockCard setSellerActiveOffersCount(int activeOffersCount) {
        card.getAsJsonObject(SELLER).addProperty("activeOffersCount", activeOffersCount);
        return this;
    }

    @Step("Добавляем seller.publicProfileLink.url = «{publicProfileUrl}»")
    public MockCard setPublicProfileUrl(String publicProfileUrl) {
        card.getAsJsonObject(SELLER).getAsJsonObject("publicProfileLink").addProperty("url", publicProfileUrl);
        return this;
    }

    @Step("Добавляем аватар")
    public MockCard setAvatar() {
        JsonObject avatar = new Gson().fromJson(getResourceAsString(AVATAR), JsonObject.class);
        card.getAsJsonObject(SELLER).add("avatar", avatar);
        return this;
    }

    @Step("Очищаем аватар")
    public MockCard removeAvatar() {
        card.getAsJsonObject(SELLER).add("avatar", null);
        return this;
    }

    public static String getCreateDateTime() {
        Calendar calendar = getCalendar();
        calendar.add(Calendar.DATE, -7);
        return getISOFormatedDate(calendar.getTime());
    }

    @Step("Добавляем «expireDateTime»")
    public MockCard setExpireDateTime() {
        card.addProperty("expireDateTime", getExpiredDateTime());
        return this;
    }

    @Step("Добавляем «editFormLink»")
    public MockCard setEditFormLink(String editFormLinkUrl) {
        JsonObject editFormLink = new JsonObject();
        editFormLink.addProperty("url", editFormLinkUrl);
        card.add("editFormLink", editFormLink);
        return this;
    }

    @Step("Добавляем адреса")
    public MockCard setAddresses(Address... addresses) {
        JsonArray addressesList = new JsonArray();
        stream(addresses).forEach(address -> addressesList.add(new Gson().toJsonTree(address).getAsJsonObject()));
        card.getAsJsonObject("contacts").add("addresses", addressesList);
        return this;
    }

    @Step("Добавляем «canonicalLink»")
    public MockCard setCanonicalLink(String link) {
        JsonObject selfLink = new JsonObject();
        selfLink.addProperty("canonicalUrl", link);
        selfLink.addProperty("route", "card");
        card.add("selfLink", selfLink);
        return this;
    }

    @Step("Добавляем описание оффера = «{description}»")
    public MockCard setDescription(String description) {
        card.addProperty("description", description);
        card.addProperty("descriptionHtml", description);
        return this;
    }

    @Step("Убираем описание оффера")
    public MockCard removeDescription() {
        card.addProperty("description", "");
        return this;
    }

    @Step("Добавляем аттрибуты")
    public MockCard setAttributes(Attribute... attributes) {
        JsonArray attributesList = new JsonArray();
        stream(attributes).forEach(attribute -> attributesList.add(new Gson().toJsonTree(attribute).getAsJsonObject()));
        card.add("attributes", attributesList);
        return this;
    }

    @Step("Удаляем атрибуты")
    public MockCard removeAttributes() {
        JsonArray attributesList = new JsonArray();
        card.add("attributes", attributesList);
        return this;
    }

    @Step("Добавляем доставку курьером = «{sendByCourier}»")
    public MockCard setSendByCourier(boolean sendByCourier) {
        card.getAsJsonObject("deliveryInfo").getAsJsonObject("selfDelivery").addProperty("sendByCourier", sendByCourier);
        return this;
    }

    @Step("Добавляем доставку по России = «{sendWithinRussia}»")
    public MockCard setSendWithinRussia(boolean sendWithinRussia) {
        card.getAsJsonObject("deliveryInfo").getAsJsonObject("selfDelivery").addProperty("sendWithinRussia", sendWithinRussia);
        return this;
    }

    @Step("Добавляем состояние = «{condition}»")
    public MockCard setCondition(BaseConstants.Condition condition) {
        card.addProperty("condition", condition.getCondition());
        return this;
    }

    @Step("Добавляем «{count}» похожих офферов")
    public MockCard addSimilarOffers(int count) {
        similarOffers = new JSONArray();
        for (int i = 0; i < count; i++) {
            similarOffers.add(mockSnippet(BASIC_SNIPPET).getMockSnippet());
        }
        return this;
    }

    @Step("Добавляем ссылку на категорию оффера «{url}»")
    public MockCard setCategoryUrl(String url) {
        card.getAsJsonObject("category").getAsJsonObject("searchLinks").getAsJsonObject("withRequestLink")
                .addProperty("url", url);
        return this;
    }

    @Step("Добавляем availableActions: «{action} = {value}»")
    public MockCard setAvaliableAction(String action, boolean value) {
        card.getAsJsonObject("availableActions").addProperty(action, value);
        return this;
    }

    public MockCard setActiveOfferAvaliableActions() {
        setAvaliableAction(REMOVE, false);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, true);
        return this;
    }

    public MockCard setBannedOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCard setBannedNoEditOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, false);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCard setInactiveOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, true);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCard setDeletedOfferAvaliableActions() {
        setAvaliableAction(REMOVE, false);
        setAvaliableAction(EDIT, false);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCard setExpiredOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, true);
        return this;
    }

    private String getExpiredDateTime() {
        Calendar calendar = getCalendar();
        calendar.add(Calendar.DATE, +23);
        return getISOFormatedDate(calendar.getTime());
    }

    public String build() {
        if (similarOffers != null) {
            JsonObject similarOffers = new GsonBuilder().create()
                    .fromJson(getResourceAsString(SIMILAR_OFFERS_TEMPLATE), JsonObject.class);
            JsonArray offersArray = new JsonArray();
            this.similarOffers.forEach(o -> offersArray.add(o.getSnippet()));
            similarOffers.addProperty("totalCount", offersArray.size());
            similarOffers.add("snippets", offersArray);
            card.add("similarOffers", similarOffers);
        }
        setCreateDateTime();
        setExpireDateTime();
        return card.toString();
    }

}
