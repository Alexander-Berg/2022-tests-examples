package ru.yandex.navi.tests;

import io.appium.java_client.MobileElement;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import ru.yandex.navi.Credentials;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.BookmarksScreen;
import ru.yandex.navi.ui.SettingsScreen;

import java.util.List;

@RunWith(RetryRunner.class)
public final class LoginSynchronizationTest extends BaseAuthTest {
    private static final Credentials credentials =
            new Credentials("jkx39414@bcaoo.com", "autotests4ever");

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Тестирование логина и синхронизации данных")
    @Ignore("MOBNAVI-17524")
    @TmsLink("navi-mobile-testing-28")  // hash: 0x00000000
    public void Тестирование_логина_и_синхронизации_данных() {
        prepare("Пользователь не авторизован. "
                + "В тестовом аккаунте изменены следующие стандартные значения настроек: "
                + "1. Меню - Настройки - Курсор. "
                + "2. Меню - Настройки - Голос. "
                + "3. Меню - Настройки - Карта и интерфейс - Дорожные события. "
                + ", а также присутствует история в разделах: "
                + "1. Поиск - История. "
                + "2. Мои места - Недавние. "
                + ", а также добавлены точки Дом/Работа и точки в Избранном", () -> {});

        login(tabBar, credentials);

        step("Перейти на экран 'Поиск'. Тапнуть на 'История'", () -> {
            tabBar.clickSearch().clickHistory();
            user.shouldNotSee("История поиска пуста");
        });

        step("Перейти на экран 'Мои места'", () -> {
            tabBar.clickBookmarks();
            user.shouldSeeAll("Алматы", "^просп. Академика Сахарова", "#Farш");
        });

        step("Тапнуть на 'Недавние'", () -> {
            BookmarksScreen.getVisible().clickRecent();
            user.shouldNotSee("История маршрутов пуста");
        });

        step("Перейти в Меню - 'Настройки'", () -> {
            SettingsScreen settingsScreen = tabBar.clickMenu().clickSettings();
            user.shouldSeeAll(Direction.DOWN, "Вера Брежнева", "Истребитель СИД");
            settingsScreen.clickClose();
        });

        step("Перейти в раздел 'Карта и интерфейс'. Нажать на 'Дорожные события'", () -> {
            tabBar.clickMenu().clickSettings().click(
                    "Карта и интерфейс", "Дорожные события", "Отображение на карте");
            SettingsScreen settingsScreen = SettingsScreen.getVisible("Отображение на карте");
            expect("Все или несколько настроек в нестандартном положении",
                    () -> checkSettings(settingsScreen.checkboxes));
            settingsScreen.clickClose();
        });

        logout(tabBar);
    }

    private static void checkSettings(List<MobileElement> checkboxes) {
        int countUnchecked = 0;
        for (MobileElement checkbox : checkboxes) {
            if (checkbox.getAttribute("checked").equals("false"))
                ++countUnchecked;
        }
        Assert.assertTrue(countUnchecked >= 2);
    }
}
