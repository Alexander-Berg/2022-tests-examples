package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.Platform;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.SearchCategory;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.LongTapMenu;
import ru.yandex.navi.ui.MastercardLiteMenu;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.TabBar;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class CloseAppTest extends BaseTest {
    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Выгрузка приложения")
    @TmsLink("navi-mobile-testing-1642")  // hash: 0xa56dd14b
    public void Выгрузка_приложения() {
        step("Загрузить приложение. Свернуть приложение по кнопке Home...", () -> {
            closeAppByHome();
            restartAppAndSkipIntro();
        });

        if (user.getPlatform() == Platform.Android) {
            step("Выйти из приложения по кнопке Back", () -> {
                closeAppByBack();
                openApp();
            });
        }

        step("Выполнить лонг-тап в любой точке карты. "
            + "В открывшемся Меню нажать кнопку 'Сюда'. "
            + "Тапнуть на кнопку 'Поехали'. "
            + "Свернуть приложение по кнопке Home. "
            + "Выгрузить из меню мультизадачности. "
            + "Запустить приложение снова", () -> {
            mapScreen.buildRouteByLongTapAndGo();
            restartAppAndSkipIntro();
            expect("Приложение запущено. "
                + "Креш при выгрузке или запуске отсутствует", () -> {});
        });

        if (user.getPlatform() == Platform.Android) {
            resetRoute();
            closeAppByHome();
            restartAppAndSkipIntro();
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Сворачивание приложения на разных экранах")
    @TmsLink("navi-mobile-testing-1643")  // hash: 0x4d894814
    public void Сворачивание_приложения_на_разных_экранах() {

        //TODO: MOBNAVI-23917
        //clickWhereToEat();
        //closeAndOpenApp();

        step("Сбросить результаты поиска...", () -> {
            step("Сбросить результаты поиска", () -> {
                GeoCard.getVisible().closeGeoCard();
                mapScreen.cancelSearch();
            });

            buildRouteAndStartSimulation();
            mapScreen.click(TabBar.OVERVIEW);
            closeAndOpenApp();
        });

        step("Тапнуть на кнопку 'Сброс'", () -> {
            OverviewScreen.getVisible().clickCancel();
            mapScreen.clickVoice();
            closeAndOpenApp();
        });

        step("Тапнуть на кнопку добавления ДС...", () -> {
            mapScreen.addRoadEvent();
            closeAndOpenApp();
        });

        step("Выполнить лонг-тап в любой точке карты...", () -> {
            mapScreen.longTap();
            closeAndOpenApp();
            LongTapMenu.getVisible().clickClose();
        });

        step("Свернуть-развернуть приложение на каждом из экранов...", () -> {
            step("Поиск", () -> {
                mapScreen.click(TabBar.SEARCH);
                closeAndOpenApp();
            });

            step("Мои места", () -> {
                mapScreen.click(TabBar.BOOKMARKS);
                closeAndOpenApp();
            });

            step("Меню", () -> {
                mapScreen.click(TabBar.MENU);
                closeAndOpenApp();
            });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({BuildCheck.class, Light.class, UnstableIos.class})
    @DisplayName("Сворачивание и выгрузка приложения")
    @TmsLink("navigator-550")  // hash: 0xb61b907d
    public void Сворачивание_и_выгрузка_приложения() {
        closeAndOpenApp();

        if (user.getPlatform() == Platform.Android) {
            step("Выйти из приложения по кнопке 'Back'", () -> {
                closeAppByBack();
                openApp();
            });
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("Выгрузка приложения на разных экранах")
    @TmsLink("navi-mobile-testing-1634")  // hash: 0x839acb1f
    public void Выгрузка_приложения_на_разных_экранах() {
        doRestartOnVariousScreens(this::restartAppAndSkipIntro);
    }

    @Test
    @Category({SkipIos.class})
    @DisplayName("Restart Navigator Activity on various screens")
    @Issue("MOBNAVI-20412")
    public void Restart_Navigator_Activity_on_various_screens() {
        doRestartOnVariousScreens(user::restartActivity);
    }

    private void doRestartOnVariousScreens(Runnable restartApp) {

        //TODO: MOBNAVI-23917
        //clickWhereToEat();
        //step("Выгрузить приложение из меню мультизадачности...", restartApp);

        step("Построить маршрут в любую точку, запустить ведение по нему...", () -> {
            buildRouteAndStartSimulation();
            mapScreen.click(TabBar.OVERVIEW);
            restartApp.run();
        });

        step("Тапнуть на кнопку добавления ДС...", () -> {
            mapScreen.addRoadEvent();
            restartApp.run();
        });

        step("Выполнить лонг-тап в любой точке карты...", () -> {
            mapScreen.longTap();
            restartApp.run();
        });

        step("Выгрузить приложение на каждом из экранов...", () -> {
            step("Поиск", () -> {
                mapScreen.click(TabBar.SEARCH);
                restartApp.run();
            });

            step("Мои места", () -> {
                mapScreen.click(TabBar.BOOKMARKS);
                restartApp.run();
            });

            step("Меню", () -> {
                mapScreen.click(TabBar.MENU);
                restartApp.run();
            });
        });
    }

    @Test
    @Ignore("MOBNAVI-23917")
    @Issue("MOBNAVI-14635")
    public void restartWithLiteMenu() {
        experiments.enable(Experiment.MASTERCARD).disable(Experiment.MAPS_SEARCH).applyAndRestart();

        tabBar.clickSearch().click(SearchCategory.DISCOUNTS);
        MastercardLiteMenu.getVisible();

        user.restartActivity();
        tabBar.checkVisible();
    }

    @Step("Тапнуть на Поиск, на любую категорию")
    private void clickWhereToEat() {
        tabBar.clickSearch().clickWhereToEatExpectGeoCard();
    }

    @Step("Свернуть приложение по кнопке Home и развернуть обратно")
    private void closeAndOpenApp() {
        closeAppByHome();
        user.waitFor(Duration.ofSeconds(2));
        user.activateApp();
    }
}
