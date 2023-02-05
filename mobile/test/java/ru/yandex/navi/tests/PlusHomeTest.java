package ru.yandex.navi.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import ru.yandex.navi.Credentials;
import ru.yandex.navi.categories.UnstableAndroid;
import ru.yandex.navi.categories.UnstableIos;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.LoginScreen;
import ru.yandex.navi.ui.PlusHomeScreen;

@RunWith(RetryRunner.class)
@Ignore("MOBNAVI-23431")
public class PlusHomeTest extends BaseAuthTest {
    private static final Credentials credentials = Credentials.AUTO_TEST_NAVI;

    @Test
    @Category({UnstableAndroid.class, UnstableIos.class})
    public void authThenPlusHomeImmediately() {
        prepare();

        launchPlusHomeIntent();
        login(LoginScreen.getVisible(), credentials);
        expectPlusHome();
    }

    @Test
    @Category({UnstableAndroid.class, UnstableIos.class})
    public void authThroughMenuThenPlusHomeAfterAppStops() {
        prepare();

        login(tabBar, credentials);

        user.stopApp();

        launchPlusHomeIntent();
        skipIntro();

        expectPlusHome();
    }

    private void expectPlusHome() {
        expect("Появилась карточка Дом Плюса", PlusHomeScreen::getVisible);
    }

    private void prepare() {
        prepare(
            "Включен Меню - Настройки - Developer Settings - Plus - Enable Plus Sdk initialization",
            () -> experiments.enable("navi_feature_init_plus_sdk").applyAndRestart()
        );
    }

    private void launchPlusHomeIntent() {
        commands.showUi("/map/plus_home");
    }
}
