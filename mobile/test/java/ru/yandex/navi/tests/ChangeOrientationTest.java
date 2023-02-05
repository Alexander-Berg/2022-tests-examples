package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.AboutScreen;
import ru.yandex.navi.ui.AliceActionsPopup;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.MenuScreen;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.LongTapMenu;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class ChangeOrientationTest extends BaseTest {
    private static final class State {
        MenuScreen menuScreen;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class})
    @DisplayName("Смена ориентации. Алиса")
    @Issue("MOBNAVI-14470")
    @TmsLink("navi-mobile-testing-304")  // hash: 0x9a8d087d
    public void Смена_ориентации_Алиса() {
        prepare(mapScreen::cancelCovidSearch);

        step("Тап на кнопку вызова голосового помощника. "
            + "Сменить ориентацию девайса.", () -> {
            mapScreen.clickVoice();
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
        });

        step("Произнести команду 'Поехали в Москву'. "
            + "Дождаться, пока покажется плашка голосового подтверждения маршрута. "
            + "Сразу после того, как она покажется, сменить ориентацию девайса. "
            + "В плашке подтверждения маршрута нажать на 'поехали'", () -> {
            askAliceToGoToMoscow();
            expect("Маршрут подтверждается. "
                + "В момент смены ориентации девайса "
                + "отображение плашки голосового помощника не ломается.",
                () -> mapScreen.checkPanelEta(Duration.ofSeconds(10)));
        });
    }

    private void askAliceToGoToMoscow() {
        int errCount = 0;
        while (true) {
            commands.askAlice("Поехали в Москву");
            OverviewScreen.waitForRoute();
            try {
                AliceActionsPopup alicePopup = AliceActionsPopup.getVisible();
                user.rotatesTo(ScreenOrientation.PORTRAIT);
                alicePopup.click("Поехали");
                break;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                ++errCount;
                System.err.println(String.format("askAliceToGoToMoscow #%d: %s", errCount, e));
                if (errCount >= 3)
                    throw e;
                OverviewScreen.getVisible().clickCancel();
                user.rotatesTo(ScreenOrientation.LANDSCAPE);
            }
        }
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации при построенном маршруте")
    @TmsLink("navi-mobile-testing-1733")  // hash: 0x080631ab
    public void Смена_ориентации_при_построенном_маршруте() {
        // 1.
        mapScreen.longTap();

        // 2.
        user.rotatesTo(ScreenOrientation.LANDSCAPE);

        // 3.
        LongTapMenu.getVisible().clickTo();

        // 4.
        user.rotatesTo(ScreenOrientation.PORTRAIT);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации при ведении по маршруту")
    @TmsLink("navi-mobile-testing-1734")  // hash: 0x73303e03
    public void Смена_ориентации_при_ведении_по_маршруту() {
        prepare("Устройство в портретном режиме. Построен и подтвержден маршрут", () -> {
            experiments.enable(Experiment.TURN_OFF_GAS_STATIONS_COVID_19_MAP_PROMO).apply();
            buildRouteAndGo(ZELENOGRAD);
        });

        // 1.
        user.rotatesTo(ScreenOrientation.LANDSCAPE);

        // 2.
        user.rotatesTo(ScreenOrientation.PORTRAIT);

        // 3.
        Dialog dialog = mapScreen.clickResetRoute();

        // 4.
        step("Повернуть телефон в ландшафтный режим. Нажать 'Да'", () -> {
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
            dialog.clickAt("Да");
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации. Карточки")
    @TmsLink("navi-mobile-testing-1736")  // hash: 0xfa7c2a1a
    public void Смена_ориентации_Карточки() {
        // Second run to skip promo banner that closes Geo-card and invokes search of gas stations
        // (each n-th launch)
        //
        experiments.disableMapsSearch().applyAndRestart();

        step("Выполнить лонгтап в любой точке на карте. "
            + "Нажать на 'Что здесь?'. "
            + "Повернуть телефон в ландшафтный режим.", () -> {
            final GeoCard geoCard = mapScreen.longTap().clickWhatIsHere();
            user.rotatesTo(ScreenOrientation.LANDSCAPE);

            expect("Открывшаяся карточка отображается внизу экрана. (Левый нижний угол). "
                + "В ней присутствует кнопка 'Поехали'.", () -> {
                geoCard.checkVisible();
                user.shouldSee(geoCard.buttonGo);
            });
        });

        step("Закрыть карточку тапом на крестик. "
            + "Открыть экран 'Поиск' по значку Лупы в нижней панели. "
            + "Тап на поисковую строку. "
            + "Сменить ориентацию девайса на портрентную.", () -> {
            GeoCard.getVisible().closeGeoCard();
            mapScreen.clickSearch().clickSearch();
            user.rotatesTo(ScreenOrientation.PORTRAIT);

            expect("Отображение клавиатуры и раздела поиска не ломаются. "
                + "На экране присутствует кнопка 'Отмена', которой мы можем закрыть окно 'Поиск'.",
                () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации. Меню")
    @TmsLink("navi-mobile-testing-1730")  // hash: 0x2f9cf714
    public void Смена_ориентации_Меню() {
        // Restart for new menu
        // TODO: prepare("Пользователь не авторизован", this::restartAppAndSkipIntro);

        final State state = new State();

        step("Запустить приложение в портретном режиме. "
            + "Тапнуть на кнопку 'Меню' в таббаре", () -> {
            state.menuScreen = tabBar.clickMenu();
            expect("Открывается контейнер Меню. "
                + "Кнопка 'Войти' отцентрирована. "
                + "Если присутствует рекламный баннер "
                + "- он не обрезан и текст в нем отображается полностью",
                () -> checkLayout(state.menuScreen));
        });

        step("Изменить ориентацию девайса на альбомную", () -> {
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
            expect("Правила отображения элементов из шага 1 не изменились",
                () -> checkLayout(state.menuScreen));
        });

        // 3.
        final MenuScreen menuScreen = state.menuScreen;
        menuScreen.clickLogin();
        user.rotatesTo(ScreenOrientation.PORTRAIT);

        // 4.
        goBack();
        menuScreen.checkVisible();
        menuScreen.clickDownloadMaps();
        user.rotatesTo(ScreenOrientation.LANDSCAPE);

        // 5.
        goBack();
        menuScreen.checkVisible();
        menuScreen.scrollDown();
        menuScreen.clickAbout();
        user.rotatesTo(ScreenOrientation.PORTRAIT);

        // 6.
        openMap(menuScreen);
        mapScreen.checkVisible();
        closeAppByHome();
        openApp();
        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        mapScreen.checkVisible();
        user.rotatesTo(ScreenOrientation.PORTRAIT);
    }

    private void checkLayout(MenuScreen menuScreen) {
        menuScreen.checkCentered(menuScreen.buttonLogin);
        if (MobileUser.isDisplayed(menuScreen.adv))
            menuScreen.checkCentered(menuScreen.adv);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации. Меню. Кнопка 'Войти'")
    @TmsLink("navi-mobile-testing-1740")  // hash: 0x9f93454f
    public void Смена_ориентации_Меню_Кнопка_Войти() {
        final MenuScreen menuScreen = prepareMenu();

        step("Тапнуть на кнопку 'Войти'. "
                + "Если открывается карточка 'Выберите аккаунт', тапнуть на 'Добавить аккаунт'",
                menuScreen::clickLogin);

        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        user.rotatesTo(ScreenOrientation.PORTRAIT);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации. Меню. Пункт 'Загрузка карт'")
    @TmsLink("navi-mobile-testing-1741")  // hash: 0x95beccc1
    public void Смена_ориентации_Меню_Пункт_Загрузка_карт() {
        final MenuScreen menuScreen = prepareMenu();

        step("Тапнуть на пункт 'Загрузка карт'. "
                + "Изменить ориентацию девайса", () -> {
            menuScreen.clickDownloadMaps();
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
        });

        user.rotatesTo(ScreenOrientation.PORTRAIT);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Смена ориентации. Меню. Пункт 'О программе'")
    @TmsLink("navi-mobile-testing-1742")  // hash: 0x97dfa4ee
    public void Смена_ориентации_Меню_Пункт_О_программе() {
        final MenuScreen menuScreen = prepareMenu();

        step("Выполнить скролл Меню вниз до конца. "
                + "Тапнуть на пункт 'О программе'",
                () -> menuScreen.clickAbout().expectCorrectLayout());

        step("Изменить ориентацию девайса на альбомную", () -> {
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
            AboutScreen.getVisible().expectCorrectLayout();
        });

        user.rotatesTo(ScreenOrientation.PORTRAIT);
    }

    @Step("Перейти на предыдущий экран")
    private void goBack() {
        user.navigateBack();
    }

    @Step("Перейти на экран 'Карта'")
    private void openMap(MenuScreen menuScreen) {
        user.navigateBack();
        menuScreen.close();
    }

    private MenuScreen prepareMenu() {
        final State state = new State();

        prepare("Пользователь не авторизован. "
                        + "Запустить приложение в портретном режиме. "
                        + "Тапнуть на кнопку 'Меню' в таббаре",
                () -> state.menuScreen = mapScreen.clickMenu());

        return state.menuScreen;
    }
}
