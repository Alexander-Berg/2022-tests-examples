package ru.yandex.navi.tests;

import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import ru.yandex.navi.Credentials;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.LoginScreen;
import ru.yandex.navi.ui.MenuScreen;

import java.time.Duration;

@RunWith(RetryRunner.class)
public class AuthTest extends BaseAuthTest {
    private static final Credentials credentials = Credentials.AUTO_TEST_NAVI;
    private static final String ACCOUNT_NAME = "Авто Тест";

    @Test
    @Category({UnstableIos.class})
    @TmsLink("navigator-1107")  // hash: 0x57541036
    public void loginLogout() {
        loginWithRotation(tabBar, credentials);
        logout(tabBar);
        removeAccount(tabBar, credentials.userName);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Авторизация")
    @TmsLink("navi-mobile-testing-15")  // hash: 0x83bda4b0
    public void Авторизация() {
        class State {
            private MenuScreen menuScreen;
            private LoginScreen loginScreen;
        }

        State state = new State();

        prepare("Пользователь не авторизован", () -> {
        });

        step("Перейти на экран 'Меню'", () -> {
            state.menuScreen = mapScreen.clickMenu();
            expect("В верхней части контейнера присутствует кнопка 'Войти'",
                () -> user.shouldSee(state.menuScreen.buttonLogin));
        });

        step("Тапнуть на 'Войти'. "
            + "Если после этого показывается список сохраненных аккаунтов: "
            + "Тапнуть на 'Добавить аккаунт", () -> {
            state.loginScreen = state.menuScreen.clickLogin();
            expect("Происходит переход на экран ввода логина",
                () -> state.loginScreen.checkVisible());
        });

        step("Ввести логин тестового аккаунта. "
            + "Тапнуть на 'Далее'. "
            + "Ввести пароль от тестового аккаунта. "
            + "Тапнуть на 'Далее'", () -> {
            login(state.loginScreen, credentials);
            expect("Происходит возврат в Меню. "
                    + "Кнопка 'Войти' не отображается. "
                    + "На ее месте отображается Имя пользователя и его аватар", () -> {
                user.shouldSee(state.menuScreen, Duration.ofSeconds(5));
                user.shouldSee(ACCOUNT_NAME, Duration.ofSeconds(5));
            });
        });

        step("Отскроллиться вниз контейнера. "
            + "Тапнуть на кнопку 'Выйти'", () -> {
            logout(state.menuScreen);
            expect("Вверху экрана 'Меню' снова отображается кнопка 'Войти'. "
                    + "Кнопка 'Выйти' не отображается",
                () -> user.shouldSee(state.menuScreen.buttonLogin));
        });
    }
}
