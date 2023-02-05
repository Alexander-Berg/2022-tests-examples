package ru.yandex.navi.tests;

import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.NaviTheme;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.OverviewScreen;

@RunWith(RetryRunner.class)
public final class NightModeTest extends BaseTest {
    private static class State {
        NaviTheme theme;
        OverviewScreen overviewScreen;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Переключение светлой и темной темы приложения")
    @TmsLink("navi-mobile-testing-1578")  // hash: 0xa1ec62f3
    public void Переключение_светлой_и_темной_темы_приложения() {
        final State state = new State();

        prepareTest();

        step("Запустить Навигатор.", () -> {
            state.theme = mapScreen.getTheme();
            expect("Интерфейс Навигатора отображается в светлой теме в светлое время суток, "
                    + "и в тёмной - в тёмное. "
                    + "Кнопки черного цвета в ночной теме и белого - в дневной. "
                    + "Цвет пробок на дорогах в ночной теме бледнее, чем в дневной", () -> {
                NaviTheme expectedTheme = NaviTheme.forNow();
                if (expectedTheme != null) {
                    Assert.assertEquals("Navi theme doesn't correspond to current time",
                            expectedTheme, state.theme);
                }
            });
        });

        step("Изменить тему Навигатора на противоположную текущей. (см. Preconditions).", () -> {
            state.theme = setTheme(NaviTheme.oppositeOf(state.theme));
            expect("Элементы интерфейса отображаются соответственно выставленной теме.",
                    () -> mapScreen.expectTheme(state.theme));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Переключение светлой и темной темы приложения в режиме ведения.")
    @TmsLink("navi-mobile-testing-1746")  // hash: 0xce67c21c
    public void Переключение_светлой_и_темной_темы_приложения_в_режиме_ведения() {
        final State state = new State();

        prepareTest();

        step("Выполнить лонг-тап в любой точке карты на расстоянии от курсора. "
                + "В открывшемся меню нажать кнопку 'Сюда'", () -> {
            state.theme = mapScreen.getTheme();
            mapScreen.longTap().clickTo();
            state.overviewScreen = OverviewScreen.waitForRoute();
            expect("Отображаемые линии вариантов маршрута соответствуют теме Навигатора.",
                    () -> mapScreen.expectTheme(state.theme));
        });

        step("Сбросить маршрут, сменить тему Навигатора на противоположную текущей. "
            + "Заново построить маршрут", () -> {
            state.overviewScreen.clickCancel();
            state.theme = setTheme(NaviTheme.oppositeOf(state.theme));
            buildRoute();

            expect("Линии альтернатив изменили цвет.", () -> mapScreen.expectTheme(state.theme));
        });

        step("Тапнуть на 'Поехали'", () -> {
            OverviewScreen.getVisible().clickGo();
            mapScreen.checkPanelEta();

            expect("Начинается ведение по маршруту. "
                    + "На экране появляются балуны со следующей улицей, "
                    + "следующим манёвром и расстоянием до ближайшего маневра. "
                    + " Эти балуны могут появляться как в углу экрана, так и на маршруте, "
                    + "указывая конкретную точку для манёвра. "
                    + "Отображается спидометр, показывающий текущую скорость пользователя. "
                    + "Появляется плашка ETA, отображающий пробки по маршруту. "
                    + "Все перечисленные элементы отображаются "
                    + "в соответствии с выставленной темой Навигатора", () -> {
                mapScreen.expectTheme(state.theme);
                // TODO:
            });
        });
    }

    private void prepareTest() {
        prepare("Включён автоматический режим смены темы Навигатора. "
                + "Данный режим выставлен по умолчанию. Изменить режим можно перейдя в Меню -> " +
                "Настройки -> Карта и интерфейс -> Ночной режим -> Авто", () -> {});
    }
}
