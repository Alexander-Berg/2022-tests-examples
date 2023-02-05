package ru.yandex.navi.tests;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Issue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.BookmarksScreen;
import ru.yandex.navi.ui.FinesScreen;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.MenuScreen;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.PayParkingScreen;
import ru.yandex.navi.ui.SettingsScreen;

import java.time.Duration;
import java.util.Map;

@RunWith(RetryRunner.class)
public final class IntentTest extends BaseTest {
    private static final String ROUTE_BYTES = "CQAAAAMAAAAxXzC2AwAACroB2gW2AQo5ChEJtCVUU1j0KE" +
            "ASBjHCoG1pbhIRCWHUzVmEXlVAEgYxwqBtaW4aEQkMit2ZyT1iQBIGMTUwwqBtKkcKCjE3LjA2LjE5LT" +
            "ASOQoL/7PxhQEAAQABAAERAAAAAAAAAAAZAAAAAAAAAAAiCwgAEYmnyNAL8+c/KgsIABEnKHlHDcnkPz" +
            "IMEAAYACAAKAAwADgAOiIKCwgBEWHUzVmEXlVAEQAAAAAAAAAAGAEgACgAMAA4AEAAIigKEgnNt7Lhx8" +
            "tCQBEAAF45ut1LQBISCdBD8VPqy0JAER34//jY3UtAMvQBCqgB4gWkAQgAEjgKEQlYsfQg4EkRQBIGMc" +
            "KgbWluEhEJm6W1xvgyFUASBjHCoG1pbhoQCX+GafBcNEFAEgUzNMKgbRowCAISFFRpbXVyYSBGcnVuem" +
            "UgU3RyZWV0GhRUaW11cmEgRnJ1bnplIFN0cmVldCIAKhIKBw0cx7FAEAIKBw1VVYVBEAEyBgoECAMYAD" +
            "oYChYIAhIGCAEQAxgDEgQIARAHEgQIARAHIigKEgnNt7Lhx8tCQBEAAF45ut1LQBISCQAAlHzUy0JAEe" +
            "Vg+irB3UtAKh0SGwoMCMrv7CMSBXymAuACEgsI1KOTNRIEW9kBcTKLAgq9AeIFuQEIABI4ChEJGcGpXj" +
            "/WHUASBjHCoG1pbhIRCfpFYanqUlNAEgYxwqBtaW4aEAkAAAC8gi9SQBIFNzLCoG0aaAgFEhJLb21zb2" +
            "1vbHNreSBBdmVudWUaGExlZnQsIEtvbXNvbW9sc2t5IEF2ZW51ZSIAKAEyMgow0L3QsCDQmtC+0LzRgd" +
            "C+0LzQvtC70YzRgdC60LjQuSDQv9GA0L7RgdC/0LXQutGCKgkKBw1VVYVBEAMyBgoECAMYAiIoChIJAA" +
            "CUfNTLQkARAABeObrdS0ASEgkAAHjI4MtCQBEAADZ6zt1LQCofEh0KDAjM9ewjEgWkA+4BXBINCKygkz" +
            "USBrIFiAOaATLHAQqAAeIFfQgAEjgKEQnEx044iUTmPxIGMMKgc2VjEhEJkYEmgEQNB0ASBjHCoG1pbh" +
            "oQCbKhDP/DY0NAEgUzOMKgbRoSCBMaDEV4aXQgKHJpZ2h0KSIAKgkKBw1VVYVBEAEyBgoECAEYAjoYCh" +
            "YIABIECAEQBRIECAEQBRIGCAEQBhgGIigKEgkAAHjI4MtCQBEAADZ6zt1LQBISCf7ImJvoy0JAEZydyc" +
            "7Y3UtAKhgSFgoJCLr77CMSAt4DEgkIgKqTNRIC9gQAAAAAAAAAAAAAAAAAAgAAAHJ1AA==";

    private static final String ROUTE_URI = "ymapsbm1://route/driving/v1/CqICCmTouOwjlw3fJPcb" +
            "mAjgHLAGqBWQPtAbgDmYMZgKgFqgQrgOwAvQFPgK8CroQLgo2BnYJrhOyCe4C-AY8BGgC5gagA2oFeAE" + 
            "-ALQDqgO_wOPHtcI5wHXAucF5wLHBV_gBOgDoARoEl7QqJM1kAjAGcgTgBaADVf_CN8Xnwn3Ec8PnwPv" +
            "HJcVlwP_AZ8CVxB4YIgBgASYCLgE0AGABqAHyAboDrgHiAofMLgJkAuYCIcR_wQH4AGADNAGmA3wAacL" +
            "hwn3CZcDGkW8Ar8CwQLAAjMyhAF9eHh2dnd4cG1lYVxYWFZQTU9MRzwvKykvUFo-J9oC1wLaAYACpQLU" +
            "AtsC2gLaAqYBpQGnAasBqwEiCAgAEgQQGBgMIgkIMRIFELUBGA8SABoAIOov";

    @Test
    public void bookmarks() {
        commands.showUi("/bookmarks");
        BookmarksScreen.getVisible();
    }

    @Test
    public void buildRouteOnMap() {
        commands.buildRoute(YANDEX, ZELENOGRAD);
        OverviewScreen.waitForRoute().clickGo();
    }

    @Test
    public void fines() {
        commands.showUi("/menu/fines");
        FinesScreen.getVisible();
    }

    @Test
    @Ignore("MOBNAVI-23917")
    public void mapSearch() {
        commands.mapSearch("Льва Толстого 16");
        GeoCard card = new GeoCard();
        user.shouldSee(card, Duration.ofSeconds(10));
    }

    @Test
    public void menu() {
        commands.showUi("/menu");
        MenuScreen.getVisible();
    }

    @Test
    @Ignore("MOBNAVI-24904")
    public void parkings() {
        commands.showUi("/map/parkings");
        user.waitForLog("parking.ui.open");
        if (!user.findElementsByText("Не удалось получить данные").isEmpty())
            return;
        PayParkingScreen.getVisible();
    }

    @Test
    public void setSoundNotifications() {
        final Map<String, String> soundNotifications = ImmutableMap.of(
                "Mute", "Выключены все звуки Навигатора",
                "Alerts", "Включены оповещения о камерах, дорожных событиях и изменениях маршрута",
                "All", "Все звуки Навигатора включены"
        );

        tabBar.clickMenu().clickSettings();

        for (Map.Entry<String, String> pair : soundNotifications.entrySet()) {
            settings.setSoundNotifications(pair.getKey());
            user.shouldSee(pair.getValue());
        }
    }

    @Test
    public void setRoute() {
        commands.setRoute(ROUTE_BYTES);
        user.waitForLog("guidance.set_route");
    }

    @Test
    public void setRouteUri() {
        commands.setRouteUri(ROUTE_URI);
        user.waitForLog("guidance.set_route");
    }

    @Test
    public void settings() {
        commands.showUi("/menu/settings");
        SettingsScreen.getVisible();
    }

    @Test
    public void settingsFromBackground() {
        user.runAppInBackground();

        commands.showUi("/menu/settings");
        SettingsScreen.getVisible();
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-17369")
    public void settingsFromStopped() {
        skipAllIntro();
        user.stopApp();

        commands.showUi("/menu/settings");
        user.waitFor(Duration.ofSeconds(5));
        skipIntro();  // possible spec. project intro: https://st.yandex-team.ru/SPNAVI-134

        user.shouldSee(new SettingsScreen(), Duration.ofSeconds(5));
    }

    @Test
    @Ignore("MOBNAVI-23917")
    // Error: Element not found: ru.yandex.navi.ui.GeoCard@142c02e4
    public void showPointOnMapFromBackground() {
        mapScreen.cancelCovidSearch();
        user.runAppInBackground();
        commands.showPointOnMap(ZELENOGRAD);
        GeoCard.getVisible();
    }

    @Test
    public void showWebView() {
        commands.showWebView("https://yandex.ru/maps/", "Welcome");
        user.shouldSeeInWebView("Карты", Duration.ofSeconds(10));
    }

    private void skipAllIntro() {
        // There are intro-screens on 3+ launch (SPNAVI-134)
        // Skip screens: ОСАГО, Грузовая маршрутизация
        //
        final int N = 3;

        for (int i = 0; i < N; ++i)
            restartAppAndSkipIntro();
    }
}
