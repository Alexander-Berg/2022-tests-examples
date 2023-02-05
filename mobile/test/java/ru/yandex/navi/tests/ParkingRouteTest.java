package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class ParkingRouteTest extends BaseTest {
    @Test
    public void buildParkingRouteByTap() {
        dismissPromoBanners();
        mapScreen.zoomIn();
        mapScreen.tapParkingButton();
        mapScreen.clickParkingRouteButton().checkPanelEta(Duration.ofSeconds(20));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение парковочного маршрута лонгтапом по кнопке Парковочного слоя")
    @TmsLink("navi-mobile-testing-1226")  // hash: 0xc91b473c
    public void Построение_парковочного_маршрута_лонгтапом_по_кнопке_Парковочного_слоя() {
        prepare("Построить маршрут.", () -> buildRouteAndGo(ZELENOGRAD));

        step("Выполнить лонгтап по кнопке парковочного слоя", () -> {
            mapScreen.longTapParkingButton();

            expect("Текущий маршрут сброшен. "
                + "Звучит аннотация 'Начинаем поиск парковки'. "
                + "Строится парковочный маршрут от текущего местоположения. "
                + "Кнопка 'Р' становится синей", () -> {
                user.waitForAnnotations(Duration.ofSeconds(10), "начинаем поиск парковки");
                // TODO: - Текущий маршрут сброшен.
                // - Строится парковочный маршрут от текущего местоположения.
                // - Кнопка 'Р' становится синей
            });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение парковочного маршрута по кнопке")
    @TmsLink("navi-mobile-testing-1227")  // hash: 0x0242be4f
    public void Построение_парковочного_маршрута_по_кнопке() {
        prepare("Включить парковочный слой тапом по кнопке P", () -> {
            dismissPromoBanners();
            mapScreen.tapParkingButton();
        });

        step("Тапнуть по кнопке построения парковочного маршрута "
            + "(находится рядом с кнопкой парковочного слоя)", () -> {
            mapScreen.clickParkingRouteButton();

            expect("Строится парковочный маршрут от текущего местоположения. "
                + "Звучит аннотация 'Начинаем поиск парковки'", () -> {
                mapScreen.checkPanelEta(Duration.ofSeconds(20));
                user.waitForAnnotations(Duration.ofSeconds(10), "начинаем поиск парковки");
            });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Обзор парковочного маршрута")
    @Issue("MOBNAVI-18437")
    @TmsLink("navi-mobile-testing-1228")  // hash: 0xf0a64d1f
    // no group_progresseta
    public void Обзор_парковочного_маршрута() {
        prepareParkingRoute(true);

        step("Тапнуть на кнопку 'Обзор' в таббаре", () -> {
            mapScreen.clickOverview();
            expect("Происходит переход на экран обзора маршрута. "
                    + "В карточке обзора маршрута отображается иконка парковочного маршрута "
                    + "и сообщение 'Маршрут для поиска парковки'",
                    () -> user.shouldSee("Маршрут для поиска парковки$"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Сброс парковочного маршрута тапом на крестик")
    @TmsLink("navi-mobile-testing-1229")  // hash: 0xf635306f
    public void Сброс_парковочного_маршрута_тапом_на_крестик() {
        prepareParkingRoute(false);

        step("Сбросить текущий маршрут тапом на крестик на плашке ETA", () -> {
            mapScreen.clickResetRoute();
            expect("Маршрут сброшен. На карте отображается парковочный слой",
                    () -> mapScreen.checkPanelEta(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Сброс парковочного маршрута через экран обзора")
    @TmsLink("navi-mobile-testing-1230")  // hash: 0x66b14a0d
    public void Сброс_парковочного_маршрута_через_экран_обзора() {
        prepareParkingRoute(false);

        step("Тапнуть на кнопку 'Обзор' в таббаре. "
                + "Тап на кнопку 'Сброс' на карточке обзора", () -> {
            mapScreen.clickOverview().clickCancel();
            expect("Маршрут сброшен. На карте отображается парковочный слой",
                    () -> mapScreen.checkPanelEta(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Парковочный маршрут")
    @TmsLink("navigator-1550")  // hash: 0x126caf2c
    public void Парковочный_маршрут() {
        prepare("Построить парковочный маршрут любым способом:. "
                + "лонгтап по кнопке парковочного слоя. "
                + "тап по кнопке построения парковочного маршрута "
                + "(находится рядом с кнопкой парковочного слоя). "
                + "Тапнуть на 'Поехали'. "
                + "Запустить симуляцию движения по маршруту", () -> {
            dismissPromoBanners();
            mapScreen.longTapParkingButton();
            toggleDebugDriving();
        });

        waitForFinish();
    }

    private void prepareParkingRoute(boolean withRestart) {
        String step = "Построить парковочный маршрут любым способом";
        if (withRestart) {
            step = "Выполнить минимум один перезапуск приложения перед выполнением тест-кейса. "
                + "Построить парковочный маршрут. "
                + "Для этого нужно активировать слой с парковками "
                + "и тапнуть кнопку построения маршрута до парковки, "
                + "которая располагается под кнопкой активации слоя с парковками";
        }

        prepare(step, () -> {
            if (withRestart)
                restartAppAndSkipIntro();
            dismissPromoBanners();
            buildRouteAndGo(ZELENOGRAD);
            mapScreen.longTapParkingButton();
        });
    }
}
