package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.Platform;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.IntroScreen;
import ru.yandex.navi.ui.PermissionAlert;

@RunWith(RetryRunner.class)
public final class IntroTest extends BaseTest {
    public IntroTest() {
        userCaps.grantPermissions = false;
    }

    @Override
    void doSkipIntro() {}

    @Test
    // TODO: archived @TmsLink("navigator-839")  // TODO: partial implementation
    @Category({UnstableIos.class})
    public void denyPermissions() {
        IntroScreen.getVisible().skip(false);
        /* TODO:
            Dialog.withTitle(user,
            "Откройте доступ к геопозиции для Навигатора, чтобы он смог вести вас по маршруту.")
            .clickAt("OK"); */
        tabBar.checkVisible();
    }

    @Test
    @Category({BuildCheck.class})
    @Issue("MOBNAVI-18056")
    public void startNavi() {
        final int MAX_RETRIES = 3;
        for (int i = 0; ; ++i) {
            boolean skippedIntro = skipIntro();
            tabBar.checkVisible();
            rotateAndReturn();
            if (!skippedIntro)
                break;
            Assert.assertTrue(String.format("Failed to start after %d retries", MAX_RETRIES),
                i < MAX_RETRIES);
            user.restartApp();
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Установка и выдача разрешений")
    @Issue("MOBNAVI-18542")
    @TmsLink("navi-mobile-testing-161")  // hash: 0xd75e433e
    public void Установка_и_выдача_разрешений() {
        prepare("Для Android версии ниже 6.0: "
            + "Навигатор НЕ установлен. "
            + "Для Android версии 6.0 и выше а также iOS: "
            + "Навигатор установлен, но ещё ни разу не запускался.", () -> {});

        if (user.getPlatform() == Platform.iOS)
            grantPermissionsIos();
        else if (user.getMajorPlatformVersion() >= 6)
            grantPermissionAndroid6Plus();
        else
            grantPermissionsAndroidLess6();

        step("Выгрузить Навигатор. "
                + "Запустить Навигатор заново.", () -> {
            user.restartApp();
            expect("Показывается ещё один или несколько интроскринов.", IntroScreen::getVisible);
        });

        step("Закрыть итроскрин тапом на кнопку 'Хорошо' или 'Понятно'.", () -> {
            IntroScreen.getVisible().skip();
            expect("Производится переход на экран карты. "
                    + "Карта открыта в районе местоположения пользователя.", () -> {
                mapScreen.cancelCovidSearch();
                tabBar.checkVisible();
            });
        });
    }

    private void grantPermissionsAndroidLess6() {
        step("ШАГИ ДЛЯ ANDROID ВЕРСИИ НИЖЕ 6.0. "
            + "Запустить установку Навигатора.", () ->
            expect("Перед установкой запрашивается набор разрешений. "
                + "Содержание набора разрешений зависит от конкретного девайса. "
                + "Обязательно присутствие следующих разрешений: "
                + "Доступ к местоположению пользователя; "
                + "Доступ к записи аудио; "
                + "Доступ к интернету.", () -> {}));

        step("Произвести и завершить установку Навигатора.", () ->
            expect("Навигатор успешно установлен.", () -> {})
        );

        step("Запустить Навигатор.", () ->
            expect("Навигатор запускается. Во время запуска показывается лончскрин "
                + "(чёрный экран с логотипом Навигатора и названием приложения). "
                + "После лончскрина показывается экран с лицензионным соглашением.", () -> {})
        );

        step("Тапнуть на кнопку 'Дальше'.", () ->
            expect("Открывается экран 'Добро пожаловать'", () -> {}));

        step("Тапнуть на 'Далее'", () ->
            expect("Происходит переход на экран 'Карта'", () -> {})
        );
    }

    private void grantPermissionAndroid6Plus() {
        final IntroScreen introScreen = IntroScreen.getVisible();

        boolean hasGoogleAPIs = user.getDriver().isAppInstalled("com.google.android.gms");

        step("ШАГИ ДЛЯ ANDROID ВЕРСИИ 6.0 И ВЫШЕ. Запустить Навигатор.",
            () -> expect("Навигатор запускается. Во время запуска показывается лончскрин "
                    + "(чёрный экран с логотипом Навигатора и названием приложения). "
                    + "После лончскрина показывается экран с лицензионным соглашением.",
                () -> {
                    if (hasGoogleAPIs)
                        user.shouldSee("Лицензионное соглашение");
                    else
                        user.shouldNotSee("Лицензионное соглашение");
                }
            ));

        step("Тапнуть на кнопку 'Далее'.", () -> {
            if (hasGoogleAPIs)
                introScreen.clickAction();
            expect("Открывается экран 'Данные о местоположении'",
                    () -> user.shouldSee("Данные о местоположении"));
        });

        step("Тапнуть на кнопку 'Без проблем'.", () -> {
            introScreen.clickAction();
            expect("Показывается запрос на предоставление доступа к местоположению. Разрешить",
                () -> PermissionAlert.getVisibleAccessLocationAlert().clickAllow());
        });

        step("Тапнуть на кнопку 'Дальше'.",
            () -> expect("Показывается интроскрин 'Помощь Алисы'",
                () -> user.shouldSee("Помощь Алисы")));

        step("Тапнуть на кнопку 'Хорошо'.", () -> {
            introScreen.clickAction();
            expect("Показывается запрос на разрешение записывать аудио. Разрешить. "
                + "Происходит переход на экран 'Карта'", () -> {
                PermissionAlert.getVisibleRecordAudioAlert().clickAllow();
                tabBar.checkVisible();
            });
        });
    }

    private void grantPermissionsIos() {
        final IntroScreen introScreen = IntroScreen.getVisible();

        step("Запустить Навигатор.",
                () -> expect("Навигатор запускается. Во время запуска показывается лончскрин "
                + "(чёрный экран с логотипом Навигатора и названием приложения). "
                + "Открывается интроскрин 'Данные о местоположении'",
                        () -> user.shouldSee("Данные о местоположении")));

        step("Тапнуть на кнопку 'Без проблем'.", () -> {
            introScreen.clickAction();
            expect("Возникает системное меню с разрешением доступа к геопозиции. Разрешить. "
                + "Открывается интроскрин 'Помощь Алисы'", () -> {
                    PermissionAlert.getVisibleAccessLocationAlert().clickAllow();
                    user.shouldSee("Помощь Алисы");
                }
            );
        });

        // Экран 'Подсказки в пути' отсутствует на эмуляторе

        step("Тапнуть на кнопку 'Дальше'.", () -> {
            introScreen.clickAction();
            expect("Возникает системное меню с разрешением доступа к микрофону. Разрешить. "
                + "Открывается интроскрин 'Уведомления'", () -> {
                PermissionAlert.getVisibleRecordAudioAlert().clickAllow();
                user.shouldSee("Уведомления");
            });
        });

        step("Тапнуть на кнопку 'Дальше'.", () -> {
            introScreen.clickAction();
            expect("Возникает системное меню с запросом на разрешение отправки уведомлении.",
                PermissionAlert::getVisibleSendNotificationAlert);
        });

        step("Разрешить отправку уведомлений", () -> {
            PermissionAlert.getVisibleSendNotificationAlert().clickAllow();
            expect("Осуществляется переход на экран карты",
                () -> tabBar.checkVisible());
        });
    }
}
