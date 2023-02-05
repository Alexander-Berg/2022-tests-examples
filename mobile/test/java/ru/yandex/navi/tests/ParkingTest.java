package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;

import ru.yandex.navi.Credentials;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.ParkingAutosScreen;
import ru.yandex.navi.ui.PayParkingScreen;
import ru.yandex.navi.ui.Pin;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class ParkingTest extends BaseAuthTest {
    @Test
    @Ignore("MOBNAVI-24904")
    @Category({UnstableIos.class})
    public void editSettings() {
        final Credentials credentials = Credentials.PARKING;

        final ParkingAutosScreen parkingAutosScreen =
            loginInMenu(tabBar, credentials).clickParking().clickAutos();
        parkingAutosScreen.checkVisible("А676АА67", "А876КК56");
        parkingAutosScreen.clickNewAuto().enterAuto("A111AA11");
    }

    @Test
    @Ignore("MOBNAVI-24904")
    public void openSettings() {
        tabBar.clickMenu().clickParking().checkVisible();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[Март 2018] Отображение слоя 'Парковки поблизости' при тапе на 'Что здесь'")
    @Ignore("MOBNAVI-12712")
    @TmsLink("navi-mobile-testing-1235")  // hash: 0x94050f43
    public void Отображение_слоя_Парковки_поблизости_при_тапе_на_Что_здесь() {
        class State {
            private GeoCard geoCard;
        }

        State state = new State();

        prepare("Выключить слой пробок. "
            + "Выключить слой парковок", () -> {
            mapScreen.cancelCovidSearch();
            mapScreen.tapTrafficButton();  // Выключить слой пробок
            showPointYandex();  // слой ПП виден также как и слой парковок на zoom>=15
        });

        step("1. Лонгтап по любому месту на карте (предпочтительнее выбирать точку в городах), "
            + "тап 'Что здесь'.", () -> {
            mapScreen.longTap().clickWhatIsHere();
            expect("На карте установился пин (открывается балун), "
                + "вокруг этого пина в радиусе 400 м отображаются парковки.", () -> {
                state.geoCard = GeoCard.getVisible();
                mapScreen.checkParkingLayer(true);
            });
        });

        step("Перетащить пин 'Что здесь' в другое место карты", () -> {
            Pin.getWhatIsHerePin().longTapAndMoveTo(0.1, 0.8);
            expect("Отображаются те же парковки, что и в шаге 1.",
                () -> mapScreen.checkParkingLayer(true));
        });

        step("Тапнуть в произвольном месте карты", () -> {
            mapScreen.tap2();
            expect("Карточка точки закрывается. "
                + "Слой ПП скрывается", () -> {
                user.shouldNotSee(state.geoCard);
                mapScreen.checkParkingLayer(false);
            });
        });
    }

    @Test
    @Category({UnstableAndroid.class, UnstableIos.class})
    @Ignore("MOBNAVI-20359")
    @Issue("MOBNAVI-20359")
    public void payParkingInLandscape() {
        dismissPromoBanners();
        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        user.waitFor(Duration.ofSeconds(2));
        dismissPromoBanners();
        showPointYandex();

        mapScreen.tapParkingButton();
        Pin.getParkingPin().tap();
        GeoCard.getVisible().clickPay();

        PayParkingScreen.getVisible().enterPhone().clickContinue();
    }
}
