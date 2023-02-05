package ru.yandex.navi.tests;

import io.qameta.allure.TmsLink;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;

@RunWith(RetryRunner.class)
public final class LandscapeTest extends BaseTest {
    public LandscapeTest() {
        userCaps.screenOrientation = ScreenOrientation.LANDSCAPE;
    }

    @Test
    @Category({Light.class, UnstableIos.class})
    @TmsLink("navigator-1342")  // hash: 0x99e5be9d
    public void launch() {
        prepare("Девайс в альбомной ориентации", () -> {
            dismissPromoBanners();
            mapScreen.cancelCovidSearch();
        });

        step("Запустить приложение",
            () -> expect("На экране 'Карта' отображаются UI-элементы: "
            + "Виджет погоды (по умолчанию выключен) / либо промобаннер заправок. "
            + "(зависит от местоположения пользователя, "
            + "например, в МСК, СПБ, Воронеже - будет промобаннер, в Туле - виджет погоды). "
            + "Кнопка установки ДС. "
            + "Алиса. "
            + "Парковки. "
            + "Светофор. "
            + "Кнопки масштаба. "
            + "Компас. "
            + "Кнопка определения местоположения. "
            + "Таббар с вкладками 'Поиск', 'Карта', 'Музыка', 'Избранное', 'Меню'", () -> {
            mapScreen.checkMainButtons();
            tabBar.checkVisible();
        }));

        step("Изменить ориентацию девайса",
            () -> expect("Отображаются те же UI-элементы, что и в шаге 1", () -> {
            user.rotatesTo(ScreenOrientation.PORTRAIT);
            mapScreen.checkMainButtons();
            tabBar.checkVisible();
        }));
    }
}
