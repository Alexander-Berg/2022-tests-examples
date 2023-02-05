package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.WaitCursor;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class WaitCursorTest extends BaseTest {
    private static final class State {
        WaitCursor waitCursor;
    }

    @Override
    void doEnd() {
        user.setAirplaneMode(false);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Работа кнопки закрыть в WaitCursor")
    @Ignore("MOBNAVI-19948")
    @Issue("MOBNAVI-16811")
    @TmsLink("navigator-1583")  // hash: 0x90a8c054
    // need Google APIs on Android 6?
    public void Работа_кнопки_закрыть_в_WaitCursor() {
        final State state = new State();

        prepare("Для ручного тестирования: "
            + "Чтобы данные дольше подтягивались и плашку было легче поймать можно включить: "
            + "Местоположение включено только по координатам сети", () -> {});

        step("Построить любой маршрут и тапнуть Поехали", () -> {
            buildRouteAndGo(ZELENOGRAD);
            expect("Маршрут построился", () -> mapScreen.checkPanelEta());
        });

        step("Включить авиарежим на девайсе. "
            + "Перезапустить приложение "
            + "и подождать появления плашки WaitCursor'а с текстом 'Поиск спутников'", () -> {
            user.setAirplaneMode(true);
            user.toggleLocationServices();
            restartAppAndSkipIntro();
            dismissSystemAlert();
            state.waitCursor = WaitCursor.getVisible();

            expect("Появилась плашка с кнопкой 'Закрыть'", () -> {
                user.shouldSee(state.waitCursor);
                state.waitCursor.checkTitle("Поиск спутников...");
                state.waitCursor.hasButton("Закрыть");
            });
        });

        step("Тапнуть на 'закрыть' на плашке", () -> {
            state.waitCursor.clickClose();
            expect("Плашка 'Поиск спутников' закрылась",
                () -> user.shouldNotSee(state.waitCursor));
        });

        step("Перестроить маршрут в любую точку", () -> {
            mapScreen.longTap().clickVia();
            state.waitCursor = WaitCursor.getVisible();
            expect("Появляется плашка 'Поиск маршрута...' с кнопкой 'Отмена'",
                () -> {
                    user.shouldSee(state.waitCursor);
                    state.waitCursor.checkTitle("Поиск маршрута...");
                    state.waitCursor.hasButton("Отмена");
                });
        });

        step("Выключить авиа-режим", () -> {
            user.setAirplaneMode(false);
            user.toggleLocationServices();
            expect("Маршрут строится, плашка wait-курсора исчезает", () -> {
                user.shouldNotSee(state.waitCursor, Duration.ofSeconds(30));
                OverviewScreen.waitForRoute();
            });
        });

        step("Тапнуть на 'Отмена'", () -> {
            OverviewScreen.getVisible().clickCancel();
            expect("Построение маршрута отменяется. "
                + "Появляется тост 'не удалось построить маршрут'", () -> {});
        });
    }

    private void dismissSystemAlert() {
        final Dialog alert = new Dialog("^Чтобы улучшить работу приложения");
        if (alert.isDisplayed()) {
            step("Закрыть системный alert", () -> alert.clickAt("Нет, спасибо"));
        }
    }
}
