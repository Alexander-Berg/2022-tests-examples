package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockNotificationsSettings {

    private static final String NOTIFICATION_SETTING_TEMPLATE = "mock/notificationSettingTemplate.json";

    public static final String PUSH = "Push";
    public static final String EMAIL = "Email";
    public static final String SMS = "Sms";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject notificationSettings;

    private MockNotificationsSettings(String pathToTemplate) {
        this.notificationSettings = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockNotificationsSettings mockNotificationSettings(String pathToResource) {
        return new MockNotificationsSettings(pathToResource);
    }

    public static MockNotificationsSettings notificationSettingsTemplate() {
        return new MockNotificationsSettings(NOTIFICATION_SETTING_TEMPLATE);
    }

    @Step("Добавляем marketingCampaignsSettings")
    public MockNotificationsSettings setMarketingCampaigns(String... campaigns) {
        JsonArray notificationChannels = new JsonArray();
        asList(campaigns).forEach(campaign -> notificationChannels.add(campaign));
        notificationSettings.getAsJsonObject("marketingCampaignsSettings").add("notificationChannels", notificationChannels);
        return this;
    }

    @Step("Добавляем chatSettings")
    public MockNotificationsSettings setChatSettings(String... chatSettings) {
        JsonArray notificationChannels = new JsonArray();
        asList(chatSettings).forEach(chatSetting -> notificationChannels.add(chatSetting));
        notificationSettings.getAsJsonObject("chatSettings").add("notificationChannels", notificationChannels);
        return this;
    }

    public String build() {
        return notificationSettings.toString();
    }

}
