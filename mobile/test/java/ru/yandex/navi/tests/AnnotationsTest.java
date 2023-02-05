package ru.yandex.navi.tests;

import com.google.common.collect.ImmutableList;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.RoutePoint;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;

import java.time.Duration;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(RetryRunner.class)
public final class AnnotationsTest extends BaseTest {
    private static final GeoPoint POINT_1 =
        new GeoPoint("Балашиха", 55.796339, 37.938199);
    private static final GeoPoint POINT_2 =
        new GeoPoint("Новодевичий монастырь", 55.733986, 37.579506);

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Аннотации о ДС в режиме Free Drive")
    @Ignore("MOBNAVI-23457")
    @TmsLink("navi-mobile-testing-390")  // hash: 0x8585e036
    public void Аннотации_о_ДС_в_режиме_Free_Drive() {
        prepare("Включены следующие настройки: "
            + "Меню -> Настройки -> Навигация -> Работа с фоном: 'с маршрутом' и 'без маршрута'",
            () -> {});

        step("Построить маршрут в любую точку, с Дорожными событиями. "
            + "Запустить симуляцию движения. Сбросить маршрут, не останавливая симуляции. "
            + "Двигаться без маршрута более 10 секунд.", () -> {
            buildRouteAndStartSimulation();
            resetRoute();
            user.waitFor(Duration.ofSeconds(10));

            expect("Происходит переход в режим Free Drive. "
                + "С экрана пропадают основные кнопки и таббар.", () -> mapScreen.checkFreeDrive());
        });

        step("Наблюдать за аннотациями.",
            () -> expect("Произносятся аннотации о дорожных событиях. "
                + "Аннотации произносятся своевременно, "
                + "до того как курсор проехал данное дорожное событие. "
                + "Аннотации о маневрах НЕ произносятся.",
                () -> user.waitForAnnotationsEx(Duration.ofMinutes(2),
                    new String[]{"камера"}, new String[]{"поверните"})));

        step("Свернуть Навигатор по системной кнопке Home. "
            + "Слушать аннотации.", () -> {
            closeAppByHome();
            expect("Аннотации о дорожных событиях по-прежнему произносятся. "
                + "Только для Android: "
                + "В шторке девайса появляется оповещение 'Включены предупреждения о камерах'.",
                () -> user.waitForAnnotations(Duration.ofMinutes(3), "камера"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class})
    @DisplayName("Ведение по маршруту. Аннотации о ДС. Камеры.")
    @TmsLink("navi-mobile-testing-395")  // hash: 0x70518eb3
    public void Ведение_по_маршруту_Аннотации_о_ДС_Камеры() {
        prepareVoice();

        step("Построить маршрут, проходящий через дорожное событие типа 'Камера'. "
            + "Тапнуть на 'Поехали'. "
            + "Запустить симуляцию движения через дебаг-панель. "
            + "Установить скорость больше ограничения на данном участке.", () -> {
            buildRouteAndGo(YANDEX, ZELENOGRAD);
            toggleDebugDriving(70);

            expect("Производится симуляция движения по маршруту. "
                + "Аннотации о маневрах и дорожных событиях произносятся.",
                () -> user.waitForAnnotations(
                    Duration.ofMinutes(3), "поверните", "впереди камера"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту. Аннотации о ДС. Школы.")
    @Issue("MOBNAVI-20207")
    @TmsLink("navi-mobile-testing-396")  // hash: 0x308503c2
    public void Ведение_по_маршруту_Аннотации_о_ДС_Школы() {
        prepareVoice();

        step("Построить маршрут, проходящий через дорожное событие типа 'Школа'. "
            + "Тапнуть на 'Поехали'. "
            + "Запустить симуляцию движения через дебаг-панель. "
            + "Установить скорость движения выше ограничения.", () -> {
            buildRouteAndGo(
                YANDEX,
                new GeoPoint("метро Фрунзенская", 55.727313, 37.580463),
                ImmutableList.of(
                    RoutePoint.way(new GeoPoint("магазин Яндекса", 55.734581, 37.588332))));
            toggleDebugDriving(90);

            expect("Производится симуляция движения по маршруту. "
                + "Аннотации о школе произносится.",
                () -> user.waitForAnnotations(Duration.ofMinutes(2), "рядом школа"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту. Автоматическое перестроение маршрута.")
    @TmsLink("navi-mobile-testing-1490")  // hash: 0x7c4d7187
    public void Ведение_по_маршруту_Автоматическое_перестроение_маршрута() {
        prepareVoice();

        buildRouteExpectAnnotations();

        step("Не сбрасывая симуляцию ведения по маршруту построить маршрут в другую точку "
            + "и подтвердить его (нажать на 'поехали'). "
            + "Дождаться расхождения построенного маршрута и симуляции.", () -> {
            buildRouteAndGo(POINT_2);
            expect("После расхождения маршрута симуляции и построенного маршрута, "
                + "звучит аннотация 'маршрут перестроен' и маршрут перестраивается.",
                () -> user.waitForAnnotations(Duration.ofMinutes(5), "маршрут перестроен"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту. Уход с маршрута.")
    @Issue("MOBNAVI-18420")
    @TmsLink("navi-mobile-testing-1491")  // hash: 0xbe3f1528
    public void Ведение_по_маршруту_Уход_с_маршрута() {
        prepareVoice();

        buildRouteExpectAnnotations();

        step("Не сбрасывая симуляцию ведения по маршруту построить маршрут в другую точку "
            + "и подтвердить его (нажать на 'поехали'). "
            + "Отключить сеть на девайсе (включить режим 'в самолёте').", () -> {
            buildRouteAndGo(POINT_2);
            user.setAirplaneMode(true);
            expect("В момент расхождения симулируемого маршрута и построенного маршрута "
                + "должна прозвучать аннотация о сходе с маршрута. "
                + "Новый маршрут не строится.",
                () -> user.waitForAnnotations(Duration.ofMinutes(5), "вы ушли с маршрута"));
        });

        step("Включить сеть на девайсе (отключить режим 'в самолёте').", () -> {
            user.setAirplaneMode(false);
            expect("Звучит аннотация 'маршрут перестроен' и маршрут перестраивается.",
                () -> user.waitForAnnotations(Duration.ofMinutes(5), "маршрут перестроен"));
        });
    }

    @Step("Выбрать Голос диктора, отличного от стандартного (Оксаны).")
    private void prepareVoice() {
    }

    private void buildRouteExpectAnnotations() {
        step("Построить маршрут имеющий несколько маневров. "
            + "Тапнуть на 'Поехали'. "
            + "Запустить симуляцию движения через дебаг-панель.", () -> {
            buildRouteAndGo(YANDEX, POINT_1);
            toggleDebugDriving();
            expect("Производится симуляция движения по маршруту. "
                    + "Аннотации о маневрах и дорожных событиях произносятся.",
                () -> user.waitForManeuverAnnotations());
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Аннотация о въезде в промежуточную точку.")
    @TmsLink("navi-mobile-testing-410")  // hash: 0xefdb2e9a
    public void Аннотация_о_въезде_в_промежуточную_точку() {
        step("Построить маршрут с промежуточной точкой. "
            + "Тапнуть на поехали. "
            + "Запустить симуляцию ведения по маршруту. "
            + "Дождаться въезда в промежуточную точку.", () -> {
            buildRouteAndGo(YANDEX, ZELENOGRAD, ImmutableList.of(RoutePoint.way(SMOLENSKAYA)));
            toggleDebugDriving(70);
            expect("Звучит аннотация 'Вы приехали в промежуточную точку'",
                () -> user.waitForAnnotations(
                    Duration.ofMinutes(5), "вы приехали в промежуточную точку"));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Отсутствие аннотации о въезде в промежуточную точку (при добавление лонгтапом)")
    @TmsLink("navi-mobile-testing-414")  // hash: 0xf60d1c79
    public void Отсутствие_аннотации_о_въезде_в_промежуточную_точку_при_добавление_лонгтапом() {
        step("Построить маршрут. "
            + "Подтвердить его. "
            + "Выполнить лонг тап по карте (где не проходит маршрут). "
            + "Тапнуть 'через'. "
            + "Тапнуть на поехали. "
            + "Запустить симуляцию ведения по маршруту. "
            + "Дождаться въезда в промежуточную точку.", () -> {
            buildRouteAndGo(YANDEX, ZELENOGRAD, ImmutableList.of(RoutePoint.via(SMOLENSKAYA)));
            toggleDebugDriving();
            expect("Не звучит аннотация 'Вы приехали в промежуточную точку'",
                () -> user.waitForAnnotationsEx(Duration.ofMinutes(3),
                    new String[]{},
                    new String[]{"вы приехали в промежуточную точку"})
            );
        });
    }
}
