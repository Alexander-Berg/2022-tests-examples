package ru.yandex.navi.tests;

import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.SettingsScreen;

import java.util.HashMap;
import java.util.Map;

@RunWith(RetryRunner.class)
public final class SettingsTest extends BaseTest {
    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Работа с настройками: сохранение после перезапуска")
    @Ignore("MOBNAVI-17524")
    @TmsLink("navi-mobile-testing-1579")  // hash: 0x1a7c03c6
    public void Работа_с_настройками_сохранение_после_перезапуска() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("Север всегда наверху", true);
        settings.put("Карта и интерфейс/Ночной режим", "Выключен");
        settings.put("Звуки и оповещения/Голос", "Алиса");
        settings.put("Навигация/Избегать платных дорог", true);
        settings.put("Уведомления/Новые голоса", false);

        step("Сменить курсор на нестандартный", () -> changeSetting("Курсор", "НЛО"));

        step("Изменить значения ключевых настроек", () -> settings.forEach(this::changeSetting));

        step("Выгрузить приложение. Запустить повторно", () -> {
            restartAppAndSkipIntro();

            verifySetting("Курсор", "НЛО");
            settings.forEach(this::verifySetting);
        });
    }

    @Step("Изменить настройку {key} -> {value}")
    private void changeSetting(String key, Object value) {
        SettingsScreen settingsScreen = mapScreen.clickMenu().clickSettings();
        settingsScreen.click(key.split("/"));
        if (value instanceof String)
            settingsScreen.click((String) value);
        tabBar.clickMap();
    }

    @Step("Проверить настройку {key} -> {value}")
    private void verifySetting(String key, Object value) {
        SettingsScreen settingsScreen = mapScreen.clickMenu().clickSettings();

        String[] path = key.split("/");
        if (path.length == 2)
            settingsScreen.click(path[0]);

        if (value instanceof String)
            user.shouldSee((String) value);

        tabBar.clickMap();
    }
}
