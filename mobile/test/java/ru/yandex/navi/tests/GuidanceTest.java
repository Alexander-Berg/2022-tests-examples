package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.RoutePoint;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.Platform;
import ru.yandex.navi.tf.RetryRunner;

import java.time.Duration;
import java.util.Collections;

@RunWith(RetryRunner.class)
public final class GuidanceTest extends BaseTest {
    private final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private final GeoPoint METRO_PARK_KULTURY
        = new GeoPoint("метро Парк Культуры", 55.735819, 37.594683);

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class})
    @DisplayName("Фоновое ведение. Выход из приложения.")
    @Issue("MOBNAVI-20412")
    @TmsLink("navi-mobile-testing-710")  // hash: 0x23de426f
    // crash MapObjectCollection
    public void Фоновое_ведение_Выход_из_приложения() {
        prepare("Фоновое ведение включено. "
            + "(В разделе Меню->Настройки->Навигация-> Работа с фоном: обе настройки включены)",
                () -> {});

        step("Построить маршрут в любую точку, имеющий несколько аннотаций и ДС на нём. "
                + "Тапнуть на 'Поехали'. "
                + "Запустить симуляцию ведения по маршруту. "
                + "Свернуть приложение, нажав кнопку Home.", () -> {
            buildRouteAndStartSimulation();
            closeAppByHome();

            expect("Аннотации о маневрах и дорожных событиях озвучиваются.",
                () -> user.waitForManeuverAnnotations());
        });

        if (user.getPlatform() == Platform.Android) {
            step("Шаг для Android: "
                    + "Развернуть приложение. "
                    + "Тапнуть по системной кнопке Back.", () -> {
                user.activateApp();
                closeAppByBack();

                expect("Приложение закрывается. "
                    + "Фоновое ведение не активируется. "
                    + "Аннотации не озвучиваются. "
                    + "Оповещения о маневрах на маршруте не отображаются в шторке девайса.", () -> {
                    user.shouldNotSeeBackgroundGuidance();
                    user.waitForAnnotationsEx(Duration.ofMinutes(1),
                        new String[]{},
                        new String[]{"камера", "поверните"});
                });
            });
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Движение без маршрута. Переход во Free Drive")
    @TmsLink("navi-mobile-testing-87")  // hash: 0xbaf6e32a
    public void Движение_без_маршрута_Переход_во_Free_Drive() {
        step("Построить маршрут, на линии которого будут располагаться Дорожные события", () ->
        {
            buildRouteAndGo(ZELENOGRAD);
            commands.toggleDebugDriving();
        });

        step("Сбросить маршрут, не останавливая симуляции.", this::resetRoute);

        step("Двигаться без маршрута более 10 секунд.", () -> {
            user.waitFor(Duration.ofSeconds(10));
            expect("Происходит переход в режим Free Drive, пропадают основные кнопки и таббар",
                () -> mapScreen.checkFreeDrive());
        });
    }

    @Category({SkipIos.class})
    @Test
    public void guidanceWithCall() {
        buildRouteToSomePointAndGo();
        commands.toggleDebugDriving();
        user.makePhoneCall();
    }

    @Test
    public void laneSigns() {
        buildRouteAndGo(null, ZELENOGRAD, Collections.singletonList(RoutePoint.way(SMOLENSKAYA)));
        commands.toggleDebugDriving();
        user.waitForLog("guidance.lane_sign_updated", Duration.ofMinutes(1));
    }

    @Test
    @Issue("MOBNAVI-15342")
    @Ignore("MOBNAVI-23917")
    @Category({UnstableIos.class})
    public void guidanceWithSearch() {
        experiments.disableMapsSearch().applyAndRestart();

        buildRouteAndGo(ZELENOGRAD);

        mapScreen.clickSearch().clickWhereToEatExpectGeoCard().closeGeoCard();

        mapScreen.tap2();  // to show map buttons
        mapScreen.zoomOut(4);
        mapScreen.checkSearchIsActive(true);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту. Скрытие элементов интерфейса.")
    @TmsLink("navi-mobile-testing-1745")  // hash: 0x6a6770b4
    public void Ведение_по_маршруту_Скрытие_элементов_интерфейса() {
        step("Построить маршрут имеющий несколько маневров. "
            + "Тапнуть на 'Поехали'. "
            + "Запустить симуляцию движения через дебаг-панель. "
            + "Не взаимодействовать с картой.", () -> {
            buildRouteAndGo(ZELENOGRAD);
            toggleDebugDriving();
            user.waitFor(WAIT_TIMEOUT);

            expect("Следующие кнопки скрываются: "
                + "кнопки масштаба. "
                + "кнопка установки ДС. "
                + "кнопка вызова голосового помощника. "
                + "кнопка компаса. "
                + "кнопка определения местоположения. "
                + "кнопка пробок и кнопка парковочного слоя. "
                + "таббар", () -> {
                user.shouldNotSee(tabBar);
                user.shouldNotSee(mapScreen.mainButtons);
            });
        });

        step("Подвигать карту.", () -> {
            mapScreen.tap2();
            expect("Взаимодействие с картой вызывает появление исчезнувших ранее кнопок.",
                () -> mapScreen.checkTabBarAndMainButtonsAppearAfterTap());
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту. Сдвиг к местоположению пользователя.")
    @TmsLink("navi-mobile-testing-1488")  // hash: 0x89cc8432
    public void Ведение_по_маршруту_Сдвиг_к_местоположению_пользователя() {
        step("Построить маршрут имеющий несколько маневров. "
            + "Тапнуть на 'Поехали'. "
            + "Запустить симуляцию движения через дебаг-панель.", () -> {
            buildRouteAndGo(ZELENOGRAD);
            toggleDebugDriving();
            expect("Производится симуляция движения по маршруту.",
                () -> mapScreen.checkPanelEta());
        });

        step("Подвигать карту так, чтобы курсор скрылся за границей карты. "
            + "Подождать, не взаимодействуя с картой.", () -> {
            swipeMapLeft();
            mapScreen.checkCursor(false);
            user.waitFor(WAIT_TIMEOUT);
            expect("Через несколько секунд происходит автосдвиг карты "
                + "к текущему местоположению курсора.", () -> mapScreen.checkCursor(true));
        });

        step("Сбросить маршрут, не завершая симуляции. "
            + "Подвигать карту так, чтобы курсор скрылся за границей карты. "
            + "Подождать, не взаимодействуя с картой.", () -> {
            mapScreen.clickResetRoute();
            swipeMapLeft();
            mapScreen.checkCursor(false);
            user.waitFor(WAIT_TIMEOUT);
            expect("Элементы интерфейса через некоторое время скрываются. "
                    + "Происходит автосдвиг к текущему местоположению курсора.",
                () -> {
                    user.shouldNotSee(mapScreen.mainButtons);
                    mapScreen.checkCursor(true);
                });
        });
    }

    private void swipeMapLeft() {
        mapScreen.swipe(Direction.LEFT);
        mapScreen.swipe(Direction.LEFT);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Ведение по маршруту")
    @TmsLink("navi-mobile-testing-1489")  // hash: 0xc9c82b59
    public void Ведение_по_маршруту() {
        prepare("Выбрать Голос диктора, отличного от стандартного (Оксаны). "
                + "Для этого надо перейти: "
                + "Меню - Настройки - Голос - Выбрать любой из списка", () -> {});

        step("Построить маршрут имеющий несколько маневров "
                + "и проходящий через несколько дорожных событий. "
                + "Тапнуть на 'Поехали'. "
                + "Запустить симуляцию движения через дебаг-панель.", () -> {
            buildRouteAndGo(METRO_PARK_KULTURY);
            toggleDebugDriving();

            expect("Производится симуляция движения по маршруту. "
                    + "Аннотации о маневрах и дорожных событиях произносятся.",
                () -> user.waitForManeuverAnnotations());
        });

        step("Дождаться завершения маршрута.",
            () -> expect("Звучит аннотация 'Вы приехали'",
                () -> user.waitForAnnotations(Duration.ofMinutes(3), "вы приехали"))
        );
    }
}
