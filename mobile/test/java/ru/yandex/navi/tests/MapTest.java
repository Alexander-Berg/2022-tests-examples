package ru.yandex.navi.tests;

import io.appium.java_client.MobileElement;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.LongTapMenu;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.Pin;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class MapTest extends BaseTest {
    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableAndroid.class, UnstableIos.class})
    @DisplayName("Скролл карты")
    @Ignore("MOBNAVI-20765")
    @TmsLink("navi-mobile-testing-871")  // hash: 0x08449e57
    public void Скролл_карты() {
        step("Перемещаем карту, проводя по экрану пальцем.", () -> {
            mapScreen.swipe(Direction.RIGHT);
            mapScreen.swipe(Direction.DOWN);
            mapScreen.swipe(Direction.RIGHT);
            expect("Приложение стабильно работает. "
                + "Карта нормально перемещается движением пальца. "
                + "Постепенно подгружаются элементы карты.", () -> {});
        });

        step("Нажать значок лупы (Поиск) внизу, "
            + "нажать на любую категорию для поиска (например, 'где поесть' ).", () -> {
            mapScreen.clickSearch().clickWhereToEatExpectGeoCard();
            expect("Карта передвигается автоматически к месту отображения результата поиска.",
                () -> {});
        });

        step("Сдвинуть карту таким образом, чтобы курсор оказался за границей экрана. "
            + "Нажать на кнопку определения местоположения", () -> {
            for (int i = 0; i < 3; ++i)
                mapScreen.swipe(Direction.LEFT);
            mapScreen.checkCursor(false);
            mapScreen.clickFindMe();
            expect("Производится сдвиг карты к текущему местоположению курсора.",
                () -> mapScreen.checkCursor(true));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Зум карты")
    @TmsLink("navi-mobile-testing-872")  // hash: 0x26fb2724
    public void Зум_карты() {
        dismissPromoBanners();

        step("Изменять масштаб карты кнопками '+' и '-' справа на экране, "
            + "быстрым двойным нажатием на экран и щипковым движением по карте двумя пальцами. "
            + "Меняем масштаб карты от самого близкого, до самого удаленного.", () -> {

            mapScreen.spread();
            mapScreen.pinch();

            zoom('+', mapScreen.plusBtn, 10);
            zoom('-', mapScreen.minusBtn, 20);

            expect("Масштабирование работает корректно. "
                + "Если приближаться или отдаляться больше некуда, "
                + "кнопка '+' или '-' становятся серее и не нажимается.", () -> {});
        });
    }

    @Step("Изменять масштаб кнопкой '{name}'")
    private void zoom(char name, MobileElement button, int count) {
        assert name == '+' || name == '-';
        for (int i = 0; i < count; ++i) {
            user.clicks(button);
            if (button.getAttribute("enabled").equals("false"))
                return;
        }
        Assert.fail("Кнопка '+' или '-' темнеет и не нажимается");
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Вращение карты")
    @TmsLink("navi-mobile-testing-873")  // hash: 0xca868e12
    public void Вращение_карты() {
        step("Поворачивать карту двумя пальцами движением по кругу. "
            + "Нажать на значок компаса внизу справа.", () -> {
            mapScreen.rotate();
            mapScreen.rotate();
            mapScreen.clickCompass();
            expect("Карта плавно поворачивается "
                + "и возвращается в состояние 'Север вверху' при нажатии на компас.",
                () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Лонгтап по пинам Избранного")
    @TmsLink("navi-mobile-testing-856")  // hash: 0x392ceff3
    public void Лонгтап_по_пинам_Избранного() {
        prepare("Пользователь авторизован. "
            + "В аккаунте есть есть сохраненные точки в Моих Местах. "
            + "Включено отображение избранных на карте: "
            + "Меню - Настройки - Карта и Интерфейс - Отображать избранное",
            () -> mapScreen.addBookmarkByLongTap("Моя точка"));

        step("Лонгтап на карте в месте, где есть пины Избранного или пины Дом/Работа.", () -> {
            Pin.getPlacePin().longTap();
            expect("Открывается лонгтап меню.", LongTapMenu::getVisible);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class})
    @DisplayName("Лонгтап по карте")
    @TmsLink("navi-mobile-testing-857")  // hash: 0xf145f905
    public void Лонгтап_по_карте() {
        step("Лонгтап на карте в любом месте", () -> {
            LongTapMenu popup = mapScreen.longTap();

            // We can't check that LongTapMenu is displayed, so try to build route...
            //
            popup.clickTo();
            OverviewScreen.waitForRoute();
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Лонгтап по пинам ДС")
    @TmsLink("navi-mobile-testing-858")  // hash: 0xd859f4ac
    public void Лонгтап_по_пинам_ДС() {
        prepare(() -> {
            mapScreen.cancelCovidSearch();
            mapScreen.zoomOut(2);
            user.waitFor(Duration.ofSeconds(2));
        });

        step("Лонгтап на карте в месте, где есть пины дорожных событий (ДС)", () -> {
            Pin.getRoadEventPin().longTap();
            expect("Открывается лонгтап меню.", LongTapMenu::getVisible);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Лонгтап по карте при открытой карточке")
    @TmsLink("navi-mobile-testing-859")  // hash: 0xf2c7882c
    public void Лонгтап_по_карте_при_открытой_карточке() {
        class State {
            private GeoCard geoCard;
        }

        final State state = new State();

        prepare(() -> {
            dismissPromoBanners();
            mapScreen.cancelCovidSearch();
            showPointYandex();
        });

        step("Тапнуть на любую иконку POI на карте", () -> {
            Pin.getPoiPin().tap();
            expect("Снизу плавно выдвинулся контейнер с карточкой организации. "
                + "В Портретной ориентации - Карточка высотой в треть экрана "
                + "(примерно, зависит от наполнения карточки). "
                + "В Альбомной ориентации - Карточка высотой во весь экран. "
                + "В карточке присутствует кнопка 'Поехали'", () -> {
                state.geoCard = GeoCard.getVisible();
                state.geoCard.hasButton("Поехали");
            });
        });

        step("Лонгтап по карте, пока открыта карточка организации", () -> {
            mapScreen.longTap();
            expect("Открывается лонгтап меню. Карточка скрыта", () -> {
                LongTapMenu.getVisible();
                user.shouldNotSee(state.geoCard);
            });
        });
    }
}
