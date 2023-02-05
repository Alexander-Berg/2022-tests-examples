package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.AboutScreen;
import ru.yandex.navi.ui.MenuScreen;
import ru.yandex.navi.ui.SettingsScreen;

import java.util.Arrays;

@RunWith(RetryRunner.class)
public final class MenuTest extends BaseTest {
    @Test
    // TODO: changed @TmsLink("navigator-879")
    public void about() {
        AboutScreen aboutScreen = tabBar.clickMenu().clickAbout();

        rotateAndReturn();

        user.clicks(aboutScreen.licenseAgreement);
        user.shouldSeeInWebView(AboutScreen.LICENSE_AGREEMENT);

        rotateAndReturn();

        user.navigateBack();

        user.clicks(aboutScreen.privacyPolicy);
        user.shouldSeeInWebView(AboutScreen.PRIVACY_POLICY);
    }

    @Test
    // TODO: changed @TmsLink("navigator-879")
    @Category(BuildCheck.class)
    public void settings() {
        MenuScreen menuScreen = tabBar.clickMenu();

        SettingsScreen settingsScreen = menuScreen.clickSettings();
        rotateAndReturn();
        user.shouldSee(settingsScreen.sounds);
        user.shouldSee(settingsScreen.fixManeuvers);
        user.shouldSee(settingsScreen.speedLimit);
        user.shouldSee(settingsScreen.voice);
        user.shouldSeeInScrollable(settingsScreen.syncSettings);

        settingsScreen.clickBack();
    }

    @Test
    public void selectVoice() {
        SettingsScreen settingsScreen = tabBar.clickMenu().clickSettings();
        settingsScreen.clickVoice().click("Алиса").clickBack();
        user.shouldSeeInScrollable(settingsScreen.alice);
    }

    @Test
    @Category({UnstableIos.class})
    public void downloadCursor() {
        SettingsScreen settingsScreen = tabBar.clickMenu().clickSettings();
        for (String cursor : Arrays.asList("Белый кот Гурмэ", "Трансформеры", "МиГ-35", "НЛО"))
            settingsScreen.clickCursor().click(cursor, Direction.DOWN).clickBack();
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-17999")
    public void downloadVoice() {
        SettingsScreen settingsScreen = tabBar.clickMenu().clickSettings();
        for (String voice : Arrays.asList("Архимаг", "Иллидан", "Белый кот Гурмэ", "Штурман"))
            settingsScreen.clickVoice().click(voice, Direction.DOWN).clickBack();
    }

    @Test
    public void fines() {
        tabBar.clickMenu().clickFines();
    }

    @Test
    @Category({UnstableIos.class})
    public void menuWithRibbon() {
        MenuScreen menuScreen = tabBar.clickMenu();

        rotateAndReturn();

        user.shouldSee(menuScreen.buttonLogin);
        user.shouldSee(menuScreen.settingsButton);
        user.shouldSee(menuScreen.addCarButton);
        user.shouldSee(menuScreen.servicesList);

        user.shouldSeeInHorizontalList(menuScreen.gas, menuScreen.servicesList);
        user.shouldSeeInHorizontalList(menuScreen.fines, menuScreen.servicesList);
        user.shouldSeeInHorizontalList(menuScreen.downloadMaps, menuScreen.servicesList);
        user.shouldSeeInHorizontalList(menuScreen.about, menuScreen.servicesList);
    }
}
