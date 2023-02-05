package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.Platform;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.EditBookmarksScreen;
import ru.yandex.navi.ui.FeedbackScreen;
import ru.yandex.navi.ui.MenuScreen;

import java.time.Duration;
import java.util.ArrayList;

@RunWith(RetryRunner.class)
public final class ContainerUiTest extends BaseTest {
    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Переход по табам. (Эксп включен)")
    @Ignore("MOBNAVI-23457")
    @TmsLink("navi-mobile-testing-1737")  // hash: 0x5a317c73
    public void Контейнерный_UI_Переход_по_табам_Эксп_включен() {
        testSwitchTabs(true);
    }

    private void testSwitchTabs(boolean usePurePlatformLayout) {
        final int N = 3;

        final String expValue = usePurePlatformLayout ? "On" : "Off";
        final String setExp = String.format(
            "%s эксперимент: Developer Settings-Misc-Use pure platform layout - %s. ",
            usePurePlatformLayout ? "Включить" : "Выключить", expValue);

        prepare("1 - Отключен поиск от МЯКа. "
            + "Developer Settings -> Search ->Y.Maps search for yandexoid -> off. "
            + "Developer Settings -> Search ->Y.Maps search screen -> off. "
            + "2 - " + setExp
            + "Перезапустить приложение",
            () -> experiments.disableMapsSearch()
                .set(Experiment.PURE_PLATFORM_LAYOUT, usePurePlatformLayout)
                .applyAndRestart());

        step("При запуске приложения - открывается экран карты. "
            + "Тап на иконку меню в тапбаре -> Настройки -> Developer Settings -> Misc", () ->
            expect("Настройка Developer Settings-Misc-Use pure platform layout - " + expValue,
                () -> {}));

        step("Поочередно тапать на кнопки 'Поиск' ,'Карта', 'Мои места'  и 'Меню' на таббаре",
            () -> {
                for (int i = 0; i < N; ++i) {
                    //TODO: MOBNAVI-23917
                    //tabBar.clickSearch();
                    tabBar.clickMap();
                    tabBar.clickBookmarks();
                    tabBar.clickMenu();
                }
                expect("Контейнер с содержимым 'выезжает' снизу плавно, без рывков", () -> {});
        });

        step("Поочередно тапать на кнопки 'Карта' и 'Мои места' на таббаре", () -> {
            for (int i = 0; i < N; ++i) {
                tabBar.clickMap();
                tabBar.clickBookmarks();
            }
            expect("Контейнер с содержимым 'выезжает' снизу плавно, без рывков", () -> {});
        });

        step("Поочередно тапать на кнопки 'Карта' и 'Меню' на нижней панели", () -> {
            for (int i = 0; i < N; ++i) {
                tabBar.clickMap();
                tabBar.clickMenu();
            }
            expect("Контейнер с содержимым 'выезжает' снизу плавно, без рывков", () -> {});
        });

        step("Переключаться кнопками на тапбаре между разделами Поиск, Мои места , Музыка и Меню",
            () -> {
                for (int i = 0; i < N; ++i) {
                    //TODO: MOBNAVI-23917
                    //tabBar.clickSearch();
                    tabBar.clickSearch();
                    tabBar.clickBookmarks();
                    tabBar.clickMusic();
                    tabBar.clickMenu();
                }
                expect("Анимация перехода между разделами отсутствует, креша нет. "
                    + "Кнопки на таббаре отображаются корректно, имеют одинаковый размер, "
                    + "не наезжают друг на друга,сам таббар находится внизу", () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Скрытие таббара")
    @TmsLink("navi-mobile-testing-1738")  // hash: 0xe6049170
    public void Контейнерный_UI_Скрытие_таббара() {
        step("Перейти на экран настроек. Пролистать содержимое экрана вниз-вверх", () -> {
            mapScreen.clickMenu().scrollDown().scrollUp();
            tabBar.checkVisible();
        });

        step("Перейти в раздел Обратная связь. Пролистать содержимое вебвью вниз-вверх.", () -> {
            FeedbackScreen feedbackScreen = MenuScreen.getVisible().clickFeedback();
            user.waitFor(Duration.ofSeconds(30));
            feedbackScreen.scrollDown().scrollUp();
            user.shouldNotSee(tabBar);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Горизонтальные свайпы внутри контейнеров")
    @TmsLink("navi-mobile-testing-1520")  // hash: 0x5ba3b194
    public void Контейнерный_UI_Горизонтальные_свайпы_внутри_контейнеров() {
        prepareDisableMapsSearch();

        //TODO: MOBNAVI-23917
        // step("Перейти на экран поиска. Swipe'ом перейти в раздел 'История'", () -> {
        //     mapScreen.clickSearch();

        //     step("Движением от правого края экрана влево перейти в раздел 'История'", () -> {
        //         user.swipe(Direction.LEFT);
        //         user.shouldSeeAnyOf("История поиска пуста", "АЗС Яндекс.Заправки");
        //     });

        //     step("Движением от левого края - вправо вернуться на экран 'Категории'", () -> {
        //         user.swipe(Direction.RIGHT);
        //         user.shouldSee("Где поесть");
        //     });
        // });

        step("Повторить предыдущий шаг для экрана 'Мои места'", () -> {
            tabBar.clickBookmarks();

            step("Движением от правого края экрана влево перейти в раздел 'Недавние'", () -> {
                user.swipe(Direction.LEFT);
                user.shouldSee("История маршрутов пуста");
            });

            step("Движением от левого края - вправо вернуться на экран 'Сохраненные'", () -> {
                user.swipe(Direction.RIGHT);
                user.shouldSee("Избранное");
            });
        });

        if (user.getPlatform() == Platform.iOS) {
            step("Шаг для IOS. "
                + "Выполнить максимальный свайп раздела влево или вправо", () -> {
                assert false;  // Not implemented
                expect("Работа приложения стабильна", () -> {
                });
            });
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Закрытие контейнера")
    @TmsLink("navi-mobile-testing-1521")  // hash: 0x85e34b1a
    public void Контейнерный_UI_Закрытие_контейнера() {
        step("Закрыть контейнер движением вниз, коснувшись верхней части контейнера с заголовком",
                () -> {
                    MenuScreen screen = mapScreen.clickMenu();
                    user.swipe(screen.buttonLogin, Direction.DOWN);
                    mapScreen.checkVisible();
                });

        if (user.getPlatform() == Platform.Android) {
            step("Перейти на экран меню, закладок или поиска. Закрыть контейнер тапом по back",
                    () -> {
                        mapScreen.clickMenu();
                        user.navigateBack();
                        mapScreen.checkVisible();
                    });
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Перемещение точек в разделе Мои Места")
    @Issue("MOBNAVI-13627")
    @TmsLink("navi-mobile-testing-1060")  // hash: 0x8a273b82
    public void Контейнерный_UI_Перемещение_точек_в_разделе_Мои_Места() {
        prepare("В 'Мои места' в раздел 'Избранное' добавлено не менее 5 точек.", () -> {
            final GeoPoint PAVLOVSK = new GeoPoint("Павловск", 59.686411, 30.431598);
            for (GeoPoint pt : new GeoPoint[]{VLADIMIR, ZELENOGRAD, PAVLOVSK, YANDEX, YAROSLAVL})
                commands.addBookmark(pt);
        });

        step("Перейти на экран 'Мои места'. Нажать на кнопку карандаша...", () -> {
            mapScreen.clickBookmarks().clickEdit();
            movePoints(3);
        });

        step("Сменить ориентацию девайса. Подвигать точки в моих местах", () -> {
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
            user.swipesUp(user.findElementByText("Избранное"));
            movePoints(2);
        });

        step("Сменить ориентацию девайса в момент сдвига позиции точки в моих местах",
            () -> user.rotatesTo(ScreenOrientation.PORTRAIT));
    }

    @Step("Подвигать точки в 'Моих местах'")
    private void movePoints(int countOfMoves) {
        EditBookmarksScreen editBookmarksScreen = EditBookmarksScreen.getVisible();
        ArrayList<String> titles = editBookmarksScreen.getTitles();

        for (int i = 0; i < countOfMoves; ++i) {
            user.swipe(editBookmarksScreen.handles.get(i), Direction.DOWN);
            ArrayList<String> newTitles = editBookmarksScreen.getTitles();
            Assert.assertNotEquals(titles.get(i), newTitles.get(i));
            titles = newTitles;
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Контейнерный UI. Переход по табам. (Эксп выключен)")
    @TmsLink("navi-mobile-testing-1749")  // hash: 0x87558db3
    public void Контейнерный_UI_Переход_по_табам_Эксп_выключен() {
        testSwitchTabs(false);
    }
}
