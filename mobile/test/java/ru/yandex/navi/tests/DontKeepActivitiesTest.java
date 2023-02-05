package ru.yandex.navi.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.time.Duration;

import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import ru.yandex.navi.categories.UnstableAndroid;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.BaseScreen;
import ru.yandex.navi.ui.BookmarksScreen;
import ru.yandex.navi.ui.MapScreen;
import ru.yandex.navi.ui.MenuScreen;
import ru.yandex.navi.ui.SearchScreen;
import ru.yandex.navi.ui.SettingsScreen;

import static junit.framework.TestCase.assertEquals;

@RunWith(RetryRunner.class)
public class DontKeepActivitiesTest extends BaseAndroidTest {
    public DontKeepActivitiesTest() {
        userCaps.initLocation = YANDEX; // For building route in some test cases
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Карта'")
    @TmsLink("navi-mobile-testing-1882")  // hash: 0xc00930d9
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_Карта() {
        runWithDontKeepActivitiesSetting(
                () -> testMinimizeAndRestoreOnGivenScreen(
                        MapScreen.class, "Карта", () -> { }));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Поиск'")
    @TmsLink("navi-mobile-testing-1883")  // hash: 0x0a7b0837
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_Поиск() {
        runWithDontKeepActivitiesSetting(
                () -> testMinimizeAndRestoreOnGivenScreen(
                        SearchScreen.class, "Поиск", () -> tabBar.clickSearch()));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Яндекс.Музыка'")
    @TmsLink("navi-mobile-testing-1884")  // hash: 0x8466f6a1
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_ЯндексМузыка() {
        // Placeholder - not auto-testing Yandex.Music-related tests
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Мои места'")
    @TmsLink("navi-mobile-testing-1885")  // hash: 0x0bd3deee
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_Мои_места() {
        runWithDontKeepActivitiesSetting(
                () -> testMinimizeAndRestoreOnGivenScreen(
                        BookmarksScreen.class, "Мои места", () -> tabBar.clickBookmarks()));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Настройки'")
    @TmsLink("navi-mobile-testing-1886")  // hash: 0xccf0c316
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_Настройки() {
        runWithDontKeepActivitiesSetting(
                () -> testMinimizeAndRestoreOnGivenScreen(
                        SettingsScreen.class, "Настройки", () -> tabBar.clickMenu().clickSettings()));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities -  сворачивание/разворачивание  на экране 'Меню'")
    @TmsLink("navi-mobile-testing-1887")  // hash: 0xdfe7aaf7
    public void Dont_keep_activities__сворачиваниеразворачивание_на_экране_Меню() {
        runWithDontKeepActivitiesSetting(
                () -> testMinimizeAndRestoreOnGivenScreen(
                        MenuScreen.class, "Меню", () -> tabBar.clickMenu()));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Карта'")
    @TmsLink("navi-mobile-testing-1888")  // hash: 0x96fdc03c
    public void Dont_keep_activities_кнопка_назад_на_экране_Карта() {
        runWithDontKeepActivitiesSetting(
                () -> testBackButtonPressOnGivenScreen(
                        MapScreen.class, "Карта", () -> { },
                        null, null));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Поиск'")
    @TmsLink("navi-mobile-testing-1889")  // hash: 0x917e55b5
    public void Dont_keep_activities_кнопка_назад_на_экране_Поиск() {
        runWithDontKeepActivitiesSetting(
                () -> testBackButtonPressOnGivenScreen(
                        SearchScreen.class, "Поиск", () -> tabBar.clickSearch(),
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Мои места'")
    @TmsLink("navi-mobile-testing-1890")  // hash: 0x12aa6a70
    public void Dont_keep_activities_кнопка_назад_на_экране_Мои_места() {
        runWithDontKeepActivitiesSetting(
                () -> testBackButtonPressOnGivenScreen(
                        BookmarksScreen.class, "Мои места", () -> tabBar.clickBookmarks(),
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Я.Музыка'")
    @TmsLink("navi-mobile-testing-1891")  // hash: 0xede2b483
    public void Dont_keep_activities_кнопка_назад_на_экране_ЯМузыка() {
        // Placeholder - not auto-testing Yandex.Music-related tests
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Меню'")
    @TmsLink("navi-mobile-testing-1892")  // hash: 0xc2aa2e10
    public void Dont_keep_activities_кнопка_назад_на_экране_Меню() {
        runWithDontKeepActivitiesSetting(
                () -> testBackButtonPressOnGivenScreen(
                        MenuScreen.class, "Меню", () -> tabBar.clickMenu(),
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - кнопка назад на экране 'Настройки'")
    @TmsLink("navi-mobile-testing-1893")  // hash: 0xa643b475
    public void Dont_keep_activities_кнопка_назад_на_экране_Настройки() {
        runWithDontKeepActivitiesSetting(
                () -> testBackButtonPressOnGivenScreen(
                        SettingsScreen.class, "Настройки", () -> tabBar.clickMenu().clickSettings(),
                        MenuScreen.class, "Меню"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Карта'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1894")  // hash: 0x699d955e
    public void Dont_keep_activities_прокидывание_интента_на_экран_Карта_Приложение_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testForegroundIntentOnGivenScreen(
                        MapScreen.class, "Карта", "map",
                        null, null));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Поиск'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1895")  // hash: 0xbde560f7
    public void Dont_keep_activities_прокидывание_интента_на_экран_Поиск_Приложение_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testForegroundIntentOnGivenScreen(
                        SearchScreen.class, "Поиск", "search",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Яндекс.Музыка'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1896")  // hash: 0x68de2e88
    public void Dont_keep_activities_прокидывание_интента_на_экран_ЯндексМузыка_Приложение_запущено() {
        // Placeholder - not auto-testing Yandex.Music-related tests
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Мои места'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1897")  // hash: 0xc3bbb6fa
    public void Dont_keep_activities_прокидывание_интента_на_экран_Мои_места_Приложение_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testForegroundIntentOnGivenScreen(
                        BookmarksScreen.class, "Мои места", "bookmarks",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Меню'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1898")  // hash: 0x38125f44
    public void Dont_keep_activities_прокидывание_интента_на_экран_Меню_Приложение_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testForegroundIntentOnGivenScreen(
                        MenuScreen.class, "Меню", "menu",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Настройки'. Приложение запущено")
    @TmsLink("navi-mobile-testing-1899")  // hash: 0x835c86cf
    public void Dont_keep_activities_прокидывание_интента_на_экран_Настройки_Приложение_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testForegroundIntentOnGivenScreen(
                        SettingsScreen.class, "Настройки", "menu/settings",
                        MenuScreen.class, "Меню"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Карта'. Приложение не запущено")
    @TmsLink("navi-mobile-testing-1900")  // hash: 0xac74f21d
    public void Dont_keep_activities_прокидывание_интента_на_экран_Карта_Приложение_не_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testBackgroundIntentOnGivenScreen(
                        MapScreen.class, "Карта", "map",
                        null, null));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Поиск'. Приложение не запущено")
    @TmsLink("navi-mobile-testing-1901")  // hash: 0x36e8f4f7
    public void Dont_keep_activities_прокидывание_интента_на_экран_Поиск_Приложение_не_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testBackgroundIntentOnGivenScreen(
                        SearchScreen.class, "Поиск", "search",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Я.Музыка'. Приложение не запущено")
    @TmsLink("navi-mobile-testing-1902")  // hash: 0x63ffa2f4
    public void Dont_keep_activities_прокидывание_интента_на_экран_ЯМузыка_Приложение_не_запущено() {
        // Placeholder - not auto-testing Yandex.Music-related tests
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Мои места'. Приложение не запущено")
    @TmsLink("navi-mobile-testing-1903")  // hash: 0x5129b3f6
    public void Dont_keep_activities_прокидывание_интента_на_экран_Мои_места_Приложение_не_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testBackgroundIntentOnGivenScreen(
                        BookmarksScreen.class, "Мои места", "bookmarks",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @DisplayName("Dont keep activities - прокидывание интента на экран 'Меню'. Приложение не запущено")
    @TmsLink("navi-mobile-testing-1904")  // hash: 0x11028189
    public void Dont_keep_activities_прокидывание_интента_на_экран_Меню_Приложение_не_запущено() {
        runWithDontKeepActivitiesSetting(
                () -> testBackgroundIntentOnGivenScreen(
                        MenuScreen.class, "Меню", "menu",
                        MapScreen.class, "Карта"));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class})
    @Ignore("MOBNAVI-22083")
    @DisplayName("Dont keep activities - сворачивание/разворачивание при введение по маршруту")
    @TmsLink("navi-mobile-testing-1981")  // hash: 0xbe5f4bdb
    public void Dont_keep_activities_сворачиваниеразворачивание_при_введение_по_маршруту() {
        runWithDontKeepActivitiesSetting(this::testMinimizeAndRestoreWhenDrivingOnRoute);
    }

    private void testMinimizeAndRestoreOnGivenScreen(
            Class<? extends BaseScreen> screenClass, String screenName, Runnable screenOpener) {

        step("do: Запустить навигатор. "
                + "Дать все разрешение(если навигатор запущен в 1 раз)."
                + "Перейти на экран '" + screenName + "'", () -> {

            restartAppAndSkipIntro();

            screenOpener.run();
        });

        expect("assert: Навигатор запускается, происходит переход на экран '" + screenName + "'", () ->
            checkScreenVisible(screenClass)
        );

        step("do: Свернуть приложение", () -> {
            closeAppByHome();
            user.waitFor(Duration.ofSeconds(2));
        });

        expect("assert: Происходит переход на рабочий стол телефона, креша нет",
                this::checkHomeScreenVisible);

        step("do: Развернуть приложение", () -> {
            user.activateApp();
            user.waitFor(Duration.ofSeconds(2));
        });

        step("assert: Приложение разворачивается на экране 'Карта'. Креша нет",
                MapScreen::getVisible);
    }

    private void testBackButtonPressOnGivenScreen(
            Class<? extends BaseScreen> screenClass, String screenName, Runnable screenOpener,
            Class<? extends BaseScreen> previousScreenClass, String previousScreenName) {

        step("do: Запустить навигатор. "
                + "Дать все разрешение(если навигатор запущен в 1 раз). "
                + "Перейти на экран '" + screenName + "'", () -> {

            restartAppAndSkipIntro();

            screenOpener.run();
        });

        expect("assert: Навигатор запускается, происходит переход на экран '" + screenName + "'", () ->
            checkScreenVisible(screenClass)
        );

        step("do: Нажать на системную кнопку 'Назад'", () -> {
            user.pressesBackButton();
            user.waitFor(Duration.ofSeconds(2));
        });

        if (previousScreenClass == null) {
            expect("assert: Происходит переход на рабочий стол телефона, креша нет",
                    this::checkHomeScreenVisible);
        } else {
            expect("assert: Происходит переход на экран '" + previousScreenName + "', креша нет", () ->
                checkScreenVisible(previousScreenClass)
            );
        }
    }

    private void testForegroundIntentOnGivenScreen(
            Class<? extends BaseScreen> screenClass, String screenName, String screenId,
            Class<? extends BaseScreen> previousScreenClass, String previousScreenName) {

        step("do: Запустить навигатор. "
                        + "Дать все разрешение(если навигатор запущен в 1 раз)",
                this::restartAppAndSkipIntro);

        expect("assert: Навигатор запускается, происходит переход на экран 'Карта'", () ->
            checkScreenVisible(MapScreen.class)
        );

        step("do: Прокинуть интент `adb shell am start -a android.intent.action.VIEW -d yandexnavi://show_ui/" + screenId + "`", () -> {
            runShellCommand("am", "start", "-a", "android.intent.action.VIEW", "-d", "yandexnavi://show_ui/" + screenId);
            user.waitFor(Duration.ofSeconds(2));
        });

        expect("assert: Происходит переход на экран '" + screenName + "', креша нет.", () ->
            checkScreenVisible(screenClass)
        );

        step("do: Тапнуть на системную кнопку назад", () -> {
            user.pressesBackButton();
            user.waitFor(Duration.ofSeconds(2));
        });

        if (previousScreenClass == null) {
            expect("assert: Происходит переход на рабочий стол телефона, креша нет",
                    this::checkHomeScreenVisible);
        } else {
            expect("assert: Происходит переход на экран '" + previousScreenName + "', креша нет", () ->
                checkScreenVisible(previousScreenClass)
            );
        }
    }

    private void testBackgroundIntentOnGivenScreen(
            Class<? extends BaseScreen> screenClass, String screenName, String screenId,
            Class<? extends BaseScreen> previousScreenClass, String previousScreenName) {

        // Clear app state before running test steps:
        prepare(() -> {
            restartAppAndSkipIntro();
            user.stopApp();
        });

        step("do: Прокинуть интент `adb shell am start -a android.intent.action.VIEW -d yandexnavi://show_ui/" + screenId + "`", () -> {
            runShellCommand("am", "start", "-a", "android.intent.action.VIEW", "-d", "yandexnavi://show_ui/" + screenId);
            user.waitFor(Duration.ofSeconds(2));
        });

        expect("assert: Навигатор запускается на экране '" + screenName + "'", () ->
            checkScreenVisible(screenClass)
        );

        step("do: Тапнуть на системную кнопку назад", () -> {
            user.pressesBackButton();
            user.waitFor(Duration.ofSeconds(2));
        });

        if (previousScreenClass == null) {
            expect("assert: Происходит переход на рабочий стол телефона, креша нет",
                    this::checkHomeScreenVisible);
        } else {
            expect("assert: Происходит переход на экран '" + previousScreenName + "', креша нет", () ->
                checkScreenVisible(previousScreenClass)
            );
        }
    }

    private void testMinimizeAndRestoreWhenDrivingOnRoute() {
        // Clear app state before running test steps:
        prepare(() -> {
            restartAppAndSkipIntro();
            user.stopApp();
        });

        step("do: Построить  любой маршрут и запустить введение по нему",
                this::buildRouteToSomePointAndGo);

        expect("assert: Маршрут строится, происходит переход в режим введение", () ->
            MapScreen.getVisible().checkPanelEta()
        );

        step("do: Свернуть приложение", () -> {
            closeAppByHome();
            user.waitFor(Duration.ofSeconds(2));
        });

        expect("assert: Приложение сворачивается, в шторке уведомлений на девайсе отображаются нотифкации введения(маневры,дс камеры)", () ->
            user.waitForManeuverAnnotations()
        );

        step("do: Развернуть приложение", () ->
            user.activateApp()
        );

        expect("assert: Приложение разворчаивается на экране 'Карта' , отображается введение по маршруту, введение происходит, креша нет", () ->
            MapScreen.getVisible().checkPanelEta()
        );
    }

    private void checkHomeScreenVisible() {
        // FIXME: Appium returns RUNNING_IN_FOREGROUND here even when app is actually in background:
        // assertEquals(user.queryAppState(), ApplicationState.RUNNING_IN_BACKGROUND);
    }

    private void checkScreenVisible(Class<? extends BaseScreen> screenClass) {
        try {
            screenClass.getMethod("getVisible").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void runWithDontKeepActivitiesSetting(Runnable runnable) {
        changeDontKeepActivitiesSetting(true);
        try {
            runnable.run();
        } finally {
            changeDontKeepActivitiesSetting(false);
        }
    }

    private void changeDontKeepActivitiesSetting(boolean enable) {
        final String setting = enable ? "1" : "0";

        // Set setting:
        runShellCommand("settings", "put", "global", "always_finish_activities", setting);
        user.waitFor(Duration.ofSeconds(1));

        // Make sure it is set:
        final String newSetting =
                runShellCommand("settings", "get", "global", "always_finish_activities")
                        .toString()
                        .trim()
                        .replaceAll("(?m)^WARNING:.*\\n", ""); // Removes all WARNING-lines
        assertEquals(newSetting, setting);
    }
}
