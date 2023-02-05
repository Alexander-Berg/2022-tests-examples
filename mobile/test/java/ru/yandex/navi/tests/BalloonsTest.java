package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.Point;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.Region;
import ru.yandex.navi.RouteColor;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.Balloon;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.Pin;

import java.time.Duration;
import java.util.List;

@RunWith(RetryRunner.class)
public final class BalloonsTest extends BaseTest {
    private static final GeoPoint LENINGRADSKIY_STATION
        = new GeoPoint("Ленинградский вокзал", 55.776452, 37.655217);
    private static final GeoPoint IVANOVO = new GeoPoint("Иваново", 57.000348, 40.973921);

    @Override
    void doEnd() {
        user.setAirplaneMode(false);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Балуны вариантов маршрута")
    @Ignore("MOBNAVI-23457")
    @Issues({@Issue("MOBNAVI-20828"), @Issue("MOBNAVI-20961")})
    @TmsLink("navi-mobile-testing-450")  // hash: 0x29b7456d
    // Problem: Finish pin not found
    public void Балуны_вариантов_маршрута() {
        stepBuildRoute(true);

        stepMoveAndZoomMap(true);

        stepMoveFinish(true);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Балуны вариантов маршрута. Тапы по балунам.")
    @Ignore("MOBNAVI-23457")
    @TmsLink("navi-mobile-testing-451")  // hash: 0xf4b50317
    public void Балуны_вариантов_маршрута_Тапы_по_балунам() {
        stepBuildRoute(true);

        step("Перебрать варианты маршрута тапом по балунам.", () -> {
            List<Balloon> balloons = Balloon.getVariantBalloons();
            Assert.assertFalse(balloons.isEmpty());
            for (Balloon balloon : balloons) {
                balloon.tap();

                expect("Активный балун выделяется цветом, "
                    + "маршрут окрашивается в цвет пробок(онлайн).", () -> {
                    Balloon activeBalloon = Balloon.getActiveVariantBalloon();
                    Assert.assertTrue("Активный балун выделяется цветом",
                        isNear(balloon, activeBalloon));
                });
            }
        });
    }

    private static boolean isNear(Balloon balloon, Balloon otherBalloon) {
        final Point center = balloon.getCenter();
        final Point otherCenter = otherBalloon.getCenter();
        return Math.abs(center.x - otherCenter.x) < 50 && Math.abs(center.y - otherCenter.y) < 50;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Балуны вариантов маршрута в оффлайне")
    @Ignore("MOBNAVI-23457")
    @TmsLink("navi-mobile-testing-452")  // hash: 0x40ff6c93
    public void Балуны_вариантов_маршрута_в_оффлайне() {
        prepare("Скачать кеши двух областей, например Тульской и Московской областей. "
            + "Настрйока Developer settings-Map- Hide active variant ballon позволяет включать, "
            + "либо выключать синие балуны в обзоре маршрута. "
            + "`Настройка Developer settings-Map- Hide active variant ballon выключена, "
            + "если она включена , то необходимо выключить и перезапустить навигатор.`", () -> {
            restartAppAndSkipIntro();  // for new Overview screen (without auto-zoom)
            downloadRegions();
        });

        step("Отключить интернет на устройстве. "
            + "Построить маршрут между регионами, "
            + "скачивание кешей которых было произведено", () -> {
            user.setAirplaneMode(true);
            buildRouteBetweenDownloadedRegions();
            expect("Произошло построение маршрута. "
                + "На экране обзора отображается один вариант маршрута в оффлайне, "
                + "окрашенный в синий цвет. К маршруту приставлен балун.", () -> {
                OverviewScreen.getVisible().checkBalloons();
                mapScreen.checkRouteColor(RouteColor.OFFLINE);
            });
        });

        stepMoveAndZoomMap(true);

        step("Двигать конечный флаг.", () -> {
            // Finish might be hidden by traffic button -- try to zoom out
            //
            mapScreen.zoomOut();

            Pin.getFinishPin().longTapAndMoveTo(0.1, 0.5);

            expect("Балун перестраиваются после совершения действий.",
                () -> OverviewScreen.waitForRoute().checkBalloons());
        });

        step("Сбросить маршрут.", () -> {
            final OverviewScreen overviewScreen = OverviewScreen.getVisible();
            overviewScreen.clickCancel();
            expect("Балун скрылся с карты вместе с маршрутами.",
                () -> overviewScreen.checkBalloons(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Балуны вариантов маршрута. Промежуточные точки.")
    @Ignore("MOBNAVI-23457")
    @Issue("MOBNAVI-21416")
    @TmsLink("navi-mobile-testing-453")  // hash: 0x21f24c60
    public void Балуны_вариантов_маршрута_Промежуточные_точки() {
        class State {
            private List<Balloon> balloons;
            private OverviewScreen overviewScreen;

            private void checkDifferent(List<Balloon> newBalloons) {
                BalloonsTest.checkDifferent(balloons, newBalloons);
                balloons = newBalloons;
            }
        }

        final State state = new State();

        prepare("Отключить настройку: "
            + "DevSettings -> Search -> Yandex Maps POI = Off. "
            + "Перезагрузить навигатор",
            () -> experiments.disable(Experiment.MAPS_POI_SEARCH).applyAndRestart());

        stepBuildRoute(true);

        step("Добавить промежуточную точку на маршрут: "
            + "выполнив лонг-тап в любом месте на карте, "
            + "затем в открывшемся Меню нажать кнопку 'Через'", () -> {
            state.balloons = Balloon.getVariantBalloons();

            // Overview screen with advertising can exceed half of screen. So use "y" < 0.5
            //
            mapScreen.longTap(0.5, 0.3).clickVia();

            expect("Балуны перестраиваются после совершения действий.", () -> {
                state.overviewScreen = OverviewScreen.waitForRoute();
                state.checkDifferent(Balloon.getVariantBalloons());
            });
        });

        step("Перемещать промежуточные точки (для этого сначала выполнить лонг-тап на точке, "
            + "а после когда маршрут изменился на пунктир - перемещать)", () -> {
            Pin.getViaPin().longTapAndMoveTo(0.3, 0.3);
            expect("Балуны перестраиваются после совершения действий.",
                () -> state.checkDifferent(Balloon.getVariantBalloons()));
        });

        step("Сбросить маршрут.", () -> {
            state.overviewScreen.clickCancel();
            expect("Все балуны скрылись с карты вместе с маршрутами.",
                () -> state.overviewScreen.checkBalloons(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Балуны вариантов маршрута в оффлайне. Промежуточные точки")
    @Ignore("MOBNAVI-23457")
    @TmsLink("navi-mobile-testing-454")  // hash: 0x83ad9486
    // no download_completed
    public void Балуны_вариантов_маршрута_в_оффлайне_Промежуточные_точки() {
        prepare("Скачать кеши двух областей, например Тульской и Московской областей. "
            + "Отключить интернет на устройстве.", () -> {
            restartAppAndSkipIntro();  // for new Overview screen (without auto-zoom)
            downloadRegions();
            user.setAirplaneMode(true);
        });

        step("Построить маршрут между регионами, скачивание кешей которых было произведено",
            () -> {
                buildRouteBetweenDownloadedRegions();
                user.waitFor(Duration.ofSeconds(2));  // wait for balloons
                expect("Произошло построение маршрута. "
                    + "На экране обзора отображается один вариант маршрута в оффлайне, "
                    + "окрашенный в синий цвет. К маршруту приставлен балун.",
                    () -> OverviewScreen.getVisible().checkBalloons());
        });

        step("Добавить промежуточную точку на маршрут: "
            + "выполнив лонг-тап в любом месте на карте, "
            + "затем в открывшемся Меню нажать кнопку 'Через'", () -> {
            mapScreen.longTap(0.3, 0.5).clickVia();
            expect("Балун перестраиваются после совершения действий.",
                () -> OverviewScreen.waitForRoute().checkBalloons());
        });

        step("Перемещать промежуточные точки (для этого сначала выполнить лонг-тап на точке, "
            + "а после когда маршрут изменился на пунктир - перемещать)", () -> {
            Pin.getAuxPin().longTapAndMoveTo(0.3, 0.3);
            expect("Балун перестраиваются после совершения действий.",
                () -> OverviewScreen.waitForRoute().checkBalloons());
        });

        step("Сбросить маршрут.", () -> {
            final OverviewScreen overviewScreen = OverviewScreen.getVisible();
            overviewScreen.clickCancel();
            expect("Балун скрылся с карты вместе с маршрутами.",
                () -> overviewScreen.checkBalloons(false));
        });
    }

    private static void checkDifferent(List<Balloon> balloons, List<Balloon> otherBallons) {
        if (balloons.size() != otherBallons.size())
            return;

        for (int i = 0; i < balloons.size(); ++i) {
            if (balloons.get(i).getCenter() != otherBallons.get(i).getCenter())
                return;
        }

        Assert.fail("Balloons not changed");
    }

    @Step("Выполнить лонг-тап в точке вдали от курсора. В открывшемся Меню нажать кнопку 'Сюда'")
    private void stepBuildRoute(boolean expectBalloons) {
        final OverviewScreen overviewScreen = buildRouteToStation();

        if (expectBalloons) {
            expect("Произошло построение маршрута в указанную точку. "
                    + "На экране обзора к каждому из вариантов приставлен баллун, "
                    + "указывающий непосредственно на данный вариант.",
                overviewScreen::checkBalloons);
        } else {
            expect("Произошло построение маршрута в указанную точку. "
                + "На экране обзора к каждому маршруту приставлены маленькие белые "
                + "или черные баллуны (в зависимости от темы) "
                + "указывающие непосредственно на данный вариант."
                + "Если присутствует несколько вариантов маршрута, "
                + "то количество баллунов будет на 1 меньше: "
                + "Например вариантов маршрутов 3, а баллуна 2. "
                + "Синий баллун не появляется", () -> { /* TODO */
                overviewScreen.checkBalloons(false);
            });
        }
    }

    @Step("Двигать и зумить карту в области маршрута")
    private void stepMoveAndZoomMap(boolean expectBalloons) {
        final OverviewScreen overviewScreen = OverviewScreen.getVisible();

        mapScreen.zoomOut(3);
        overviewScreen.checkBalloons(expectBalloons);
        mapScreen.zoomIn(3);
        overviewScreen.checkBalloons(expectBalloons);

        /* TODO:
        mapScreen.swipe(Direction.LEFT);
        mapScreen.swipe(Direction.RIGHT);
         */

        if (expectBalloons) {
            expect("Балуны значительно не пересекают маршрут, "
                + "умещаются в экран полностью. "
                + "Перестраиваются по необходимости раз в две секунды "
                + "или по окончанию движения камеры.", overviewScreen::checkBalloons);
        } else {
            expect("Баллуны не появляются", () -> overviewScreen.checkBalloons(false));
        }
    }

    @Step("Двигать конечный флаг.")
    private void stepMoveFinish(boolean expectBalloons) {
        final OverviewScreen overviewScreen = OverviewScreen.getVisible();

        // Finish might be hidden by traffic button -- try to zoom out
        //
        mapScreen.zoomOut();

        Pin.getFinishPin().longTapAndMoveTo(0.5, 0.5);
        OverviewScreen.waitForRoute();

        final String expectation = expectBalloons
            ? "Балуны перестраиваются после совершения действий." : "Балуны не появляются";
        expect(expectation, () -> overviewScreen.checkBalloons(expectBalloons));
    }

    private void downloadRegions() {
        settings.setOfflineCacheWifiOnly(false);
        downloadCache(Region.VLADIMIR);
        downloadCache(Region.IVANOVO);
    }

    private OverviewScreen buildRouteToStation() {
        showPoint(BalloonsTest.LENINGRADSKIY_STATION);
        mapScreen.longTap(0.5, 0.5).clickTo();
        return OverviewScreen.waitForRoute();
    }

    private void buildRouteBetweenDownloadedRegions() {
        buildRoute(VLADIMIR, IVANOVO);
    }
}
