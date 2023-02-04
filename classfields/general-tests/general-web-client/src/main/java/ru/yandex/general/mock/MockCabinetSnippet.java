package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.Statistics;
import ru.yandex.general.beans.card.StatisticsGraphItem;
import ru.yandex.general.consts.CardStatus;

import java.util.Calendar;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getISOFormatedDate;
import static ru.yandex.general.utils.Utils.getStatisticsGraph;

public class MockCabinetSnippet {

    public static final String BASIC_SNIPPET = "mock/cabinetSnippet/basicSnippet.json";
    public static final String REZUME_SNIPPET = "mock/cabinetSnippet/rezumeSnippet.json";
    private static final String PREVIEW_PHOTO_ITEM = "mock/cabinetSnippet/photoPreviewItem.json";

    private static final String TYPENAME = "__typename";
    private static final String PRICE_RUR = "priceRur";
    private static final String PRICE = "price";
    private static final String CURRENT_PRICE = "currentPrice";
    private static final String STATUS = "status";
    private static final String TYPE = "type";
    private static final String TITLE = "title";
    private static final String REASON = "reason";
    private static final String REMOVE = "remove";
    private static final String EDIT = "edit";
    private static final String ACTIVATE = "activate";
    private static final String HIDE = "hide";
    public static final String ACTUALIZE = "actualize";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject snippet;

    private MockCabinetSnippet(String pathToSnippet) {
        this.snippet = new Gson().fromJson(getResourceAsString(pathToSnippet), JsonObject.class);
    }

    public static MockCabinetSnippet mockSnippet(String pathToSnippet) {
        return new MockCabinetSnippet(pathToSnippet);
    }

    @Step("Добавляем price = «{price}»")
    public MockCabinetSnippet setPrice(long price) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(PRICE_RUR, price);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "InCurrency");
        return this;
    }

    @Step("Добавляем зарплату = «{sallaryPrice}»")
    public MockCabinetSnippet setSallaryPrice(String sallaryPrice) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Salary");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty("salaryRur", sallaryPrice);
        return this;
    }

    @Step("Устанавливаем стоимость - даром")
    public MockCabinetSnippet setFreePrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Free");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем стоимость - цена не указана")
    public MockCabinetSnippet setUnsetPrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Unset");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Добавляем availableActions: «{action} = {value}»")
    public MockCabinetSnippet setAvaliableAction(String action, boolean value) {
        snippet.getAsJsonObject("availableActions").addProperty(action, value);
        return this;
    }

    public MockCabinetSnippet setActiveOfferAvaliableActions() {
        setAvaliableAction(REMOVE, false);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, true);
        return this;
    }

    public MockCabinetSnippet setBannedOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCabinetSnippet setBannedNoEditOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, false);
        setAvaliableAction(ACTIVATE, false);
        setAvaliableAction(HIDE, false);
        return this;
    }

    public MockCabinetSnippet setInactiveOfferAvaliableActions() {
        setAvaliableAction(REMOVE, true);
        setAvaliableAction(EDIT, true);
        setAvaliableAction(ACTIVATE, true);
        setAvaliableAction(HIDE, false);
        return this;
    }

    @Step("Добавляем status.type = «{type}»")
    public MockCabinetSnippet setStatusType(String type) {
        snippet.getAsJsonObject(STATUS).addProperty(TYPE, type);
        return this;
    }

    @Step("Добавляем «Inactive» статус, с причиной отлючения модерацией «Вы долго не выходили на связь»")
    public MockCabinetSnippet setCantCallInactiveStatus() {
        JsonObject reason = new JsonObject();

        reason.addProperty(TYPE, "ModerationReason");
        reason.addProperty("reasonText", "Мы не смогли с вами связаться, поэтому сняли объявление с публикации. Если предложение актуально, нажмите <b>Активировать</b> и постарайтесь быть на связи. Например, укажите в описании время, в которое вам удобно принимать звонки или сообщения.");
        reason.addProperty(TITLE, "Вы долго не выходили на связь");

        snippet.getAsJsonObject(STATUS).add(REASON, reason);
        setStatusType(INACTIVE);
        return this;
    }

    @Step("Добавляем «Banned» статус с причинами бана")
    public MockCabinetSnippet setBannedWithReason(CardStatus.OfferBanReasons... banReasons) {
        JsonArray reasons = new JsonArray();
        JsonObject description = new JsonObject();

        for (CardStatus.OfferBanReasons banReason : banReasons) {
            JsonObject reason = new JsonObject();

            reason.addProperty("code", banReason.getCode());
            reason.addProperty(TITLE, banReason.getTitle());
            reason.addProperty(REASON, banReason.getReason());

            reasons.add(reason);
        }

        description.add("reasons", reasons);
        snippet.getAsJsonObject(STATUS).add("description", description);
        setStatusType(BANNED);
        return this;
    }

    @Step("Добавляем «Inactive» статус, с причиной «{deactivateStatus}»")
    public MockCabinetSnippet setInactiveWithReason(CardStatus.CardDeactivateStatuses deactivateStatus) {
        JsonObject reason = new JsonObject();

        setStatusType(INACTIVE);
        reason.addProperty(TYPE, deactivateStatus.getMockValue());
        snippet.getAsJsonObject(STATUS).add(REASON, reason);
        return this;
    }

    @Step("Добавляем «statisticsGraph» за последние «{daysCount}» дней")
    public MockCabinetSnippet setStatisticsGraph(int daysCount) {
        snippet.add("statisticsGraph", getStatisticsGraph(daysCount));
        return this;
    }

    @Step("Добавляем «statisticsGraph» за последние «{daysCount}» дней")
    public MockCabinetSnippet setStatisticsGraph(List<StatisticsGraphItem> statisticsGraphItems) {
        JsonArray records = new JsonArray();
        JsonObject statisticsGraph = new JsonObject();

        statisticsGraphItems.stream().forEach(graphItem -> {
            records.add(new Gson().toJsonTree(graphItem).getAsJsonObject());
        });
        statisticsGraph.add("records", records);

        snippet.add("statisticsGraph", statisticsGraph);
        return this;
    }

    @Step("Добавляем «editFormLink»")
    public MockCabinetSnippet setEditFormLink(String editFormLinkUrl) {
        JsonObject editFormLink = new JsonObject();
        editFormLink.addProperty("url", editFormLinkUrl);
        snippet.add("editFormLink", editFormLink);
        return this;
    }

    @Step("Добавляем statistics.today")
    public MockCabinetSnippet setTodayStatistics(Statistics statistics) {
        snippet.getAsJsonObject("statistics").add("today", new Gson().toJsonTree(statistics).getAsJsonObject());
        return this;
    }

    @Step("Добавляем statistics.total")
    public MockCabinetSnippet setTotalStatistics(Statistics statistics) {
        snippet.getAsJsonObject("statistics").add("total", new Gson().toJsonTree(statistics).getAsJsonObject());
        return this;
    }

    @Step("Добавляем VAS")
    public MockCabinetSnippet setVas() {
        JsonArray purchasableProducts = new JsonArray();
        JsonObject vas = new JsonObject();
        vas.addProperty("productCode", "raise_1");
        vas.addProperty("priceKopecks", "4900");
        vas.addProperty("productType", "FreeRaiseVas");
        purchasableProducts.add(vas);
        snippet.add("purchasableProducts", purchasableProducts);
        return this;
    }

    @Step("Добавляем cardLink.url = «{url}»")
    public MockCabinetSnippet setCardLinkUrl(String url) {
        snippet.getAsJsonObject("cardLink").addProperty("url", url);
        return this;
    }

    @Step("Добавляем publishDateTime = «{days}» дней от текущей даты")
    public MockCabinetSnippet setPublishDateTime(int days) {
        snippet.addProperty("publishDateTime", getDateTime(days));
        return this;
    }

    @Step("Добавляем expireDateTime = «{days}» дней от текущей даты")
    public MockCabinetSnippet setExpireDateTime(int days) {
        snippet.addProperty("expireDateTime", getDateTime(days));
        return this;
    }

    @Step("Добавляем название оффера = «{title}»")
    public MockCabinetSnippet setTitle(String title) {
        snippet.addProperty("title", title);
        return this;
    }

    @Step("Добавляем «{count}» фото")
    public MockCabinetSnippet addPhoto(int count) {
        JsonArray photos = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonObject photoItem = new GsonBuilder().create().fromJson(
                    getResourceAsString(PREVIEW_PHOTO_ITEM), JsonObject.class);
            photos.add(photoItem);
        }
        snippet.getAsJsonObject().add("photos", photos);
        return this;
    }

    @Step("Удаляем фотографии")
    public MockCabinetSnippet removePhotos() {
        JsonArray photos = new JsonArray();
        snippet.add("photos", photos);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockCabinetSnippet setId(String id) {
        snippet.addProperty("id", id);
        return this;
    }

    public static String getDateTime(int days) {
        Calendar calendar = getCalendar();
        calendar.add(Calendar.DATE, days);
        return getISOFormatedDate(calendar.getTime());
    }

}
