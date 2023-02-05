package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.Region;
import ru.yandex.navi.RouteColor;
import ru.yandex.navi.SearchCategory;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.SavedDataScreen;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.LongTapMenu;

import java.util.regex.Pattern;

// https://testpalm.yandex-team.ru/testcase/navigator-562
@SuppressWarnings("NonAsciiCharacters")
@RunWith(RetryRunner.class)
@Category(SkipIos.class)
public final class OfflineTest extends BaseTest {
    public OfflineTest() {
        userCaps.initLocation = VLADIMIR;
    }

    private static class State {
        GeoCard geoCard;
        LongTapMenu longTapMenu;
    }

    @Override
    void doEnd() {
        user.setAirplaneMode(false);
    }

    @Test
    // TODO: changed @TmsLink("navigator-915")
    public void route() {
        settings.setOfflineCacheWifiOnly(false);
        downloadCache(Region.VLADIMIR);
        downloadCache(Region.YAROSLAVL);

        user.setAirplaneMode(true);

        buildRouteAndGo(VLADIMIR, YAROSLAVL);
    }

    @Test
    @Ignore("MOBNAVI-23917")
    // TODO: archived? @TmsLink("navigator-915")
    @Category({UnstableIos.class})
    public void search() {
        prepare(() -> {
            mapScreen.clickFindMe();
            settings.setOfflineCacheWifiOnly(false);
            downloadCache(Region.VLADIMIR);
            user.setAirplaneMode(true);
        });

        mapScreen.clickSearch().clickWhereToEatExpectGeoCard().closeGeoCard();

        mapScreen.buildRouteBySearchAndGo("Суздаль");
    }

    @Test
    // TODO: changed @TmsLink("navigator-915")
    public void clearCache() {
        final int KB = 1024;
        final int MB = 1024 * KB;

        settings.setOfflineCacheWifiOnly(false);
        downloadCache(Region.KRASNOYARSK);

        SavedDataScreen screen = tabBar.clickMenu().clickSettings().clickSavedData();
        Assert.assertTrue(screen.getSizeOfMaps() > 20 * MB);

        screen.clickClearMaps();

        Dialog.withTitle("^Удалить все просмотренные фрагменты карт")
                .clickAt("Да");

        Assert.assertEquals(20 * KB, screen.getSizeOfMaps());
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableAndroid.class, UnstableIos.class})
    @DisplayName("Тестирование в офлайне - Построение маршрута")
    @Ignore("MOBNAVI-19976")
    @Issue("MOBNAVI-18708")
    @TmsLink("navi-mobile-testing-635")  // hash: 0x3bf2a28d
    // missed "show-variants"
    // Assertion: Route color is ONLINE
    public void Тестирование_в_офлайне_Построение_маршрута() {
        prepare("Оффлайн кеш не скачан", () -> settings.setOfflineCacheWifiOnly(false));

        step("Включить режим 'в самолете'. "
                + "Сделать лонгтап (долгое нажатие) в любую точку карты. "
                + "В появившемся меню нажать на пункт 'Сюда'.", () -> {
            user.setAirplaneMode(true);
            mapScreen.longTap().clickTo();

            expect("В верхней части экрана появляется надпись 'Поиск маршрута'.",
                    () -> user.waitForLog("driving.request"));
        });

        step("Выключить режим 'в самолете'", () -> {
            user.setAirplaneMode(false);
            expect("Строится маршрут от вашего местоположения до указанной точки.",
                    () -> OverviewScreen.waitForRoute().clickGo());
        });

        step("Скачать оффлайн кеш любого региона. "
                + "Перейти в Настройки- Загрузка карт. "
                + "Ввести в поисковой строке 'Москва'. "
                + "Тапнуть найденному варианту. "
                + "Тапнуть по кнопке загрузки офлайн кеша. "
                + "Дождаться завершения загрузки", () -> downloadCache(Region.VLADIMIR));

        step("Перейти на экран карты. "
                + "Подвигать карту так, чтобы на экране отображался город, "
                + "кеш которого был скачан. "
                + "Включить авиарежим. "
                + "Выполнить лонгтап по свободному месту на карте. "
                + "Тап на кнопку 'отсюда'. "
                + "Выполнить лонгтап по другому свободному месту на карте. "
                + "Тап на кнопку 'сюда'.", () -> {
            user.setAirplaneMode(true);
            mapScreen.longTap(0.3, 0.1).clickFrom();
            mapScreen.longTap(0.7, 0.7).clickTo();
            expect("Строится оффлайн маршрут. "
                + "Маршрут окрашен синим цветом. "
                + "ЕТА отображатеся синим цветом. "  // TODO
                + "Отображается уведомление 'Маршрут без интернета'", () -> {
                OverviewScreen.waitForRoute();
                mapScreen.checkRouteColor(RouteColor.OFFLINE);
                user.shouldSee(mapScreen.offlineResultsPanel);
            });
        });

        step("Выключить режим 'в самолёте'. "
            + "Тап по кнопке 'маршрут без интернета'", () -> {
            user.setAirplaneMode(false);
            mapScreen.offlineResultsPanel.tryClick();

            expect("Построенный маршрут окрашивается в цвета пробок. "
                + "(Нажатия на кнопку 'маршрут без интернета' может не понадобиться - "
                + "маршрут может окраситься в цвета пробок самостоятельно, "
                + "а кнопка в этом случае пропадёт)",
                () -> mapScreen.checkRouteColor(RouteColor.ONLINE));
        });

        step("Скачать кеш соседнего региона. "
                + "Отключить сеть. "
                + "Построить маршрут из одного региона в другой. "
                + "Нажать на 'поехали'", () -> {
            downloadCache(Region.YAROSLAVL);
            buildRouteAndGo(VLADIMIR, YAROSLAVL);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Тестирование в офлайне - Поиск. Кеши не скачаны.")
    @TmsLink("navi-mobile-testing-592")  // hash: 0x7de3498d
    public void Тестирование_в_офлайне_Поиск_Кеши_не_скачаны() {
        prepare("Офлайн кеши не скачаны. "
            + "Надо отключить эксперимент, если он включен: "
            + "Developer settings в разделе Search - Yandex Maps search screen - off. "
            + "Перезагрузить приложение", () -> {
            experiments.disableMapsSearch().applyAndRestart();
            mapScreen.clickFindMe();
        });

        step("Открыть Яндекс.Навигатор. "
                + "Включить режим 'В самолёте'. "
                + "Убедиться, что wifi и мобильное соединение выключены. "
                + "Открыть экран 'Поиск' по значку 🔍 в нижней панели. "
                + "Нажать на категорию, например 'где поесть'", () -> {
            user.setAirplaneMode(true);
            mapScreen.clickSearch().click(SearchCategory.WHERE_TO_EAT);
            expect("Появляется сообщение 'Не удалось выполнить поиск'",
                () -> mapScreen.checkSearchIsActive(false));
        });

        step("Выключить режим 'В самолете'. Дождаться подключения WiFi. "
                + "Открыть экран 'Поиск' по значку 🔍 в нижней панели. "
                + "Нажать на категорию 'где поесть'", () -> {
            user.setAirplaneMode(false);
            dismissOfflineSearchDialog();
            mapScreen.clickSearch().clickWhereToEatExpectGeoCard().closeGeoCard();
            expect("Производится поиск по категории, предлагается несколько вариантов.",
                () -> mapScreen.checkSearchIsActive(true));
        });
    }

    private void dismissOfflineSearchDialog() {
        Dialog dialog = new Dialog("^Ищите места и стройте маршруты");
        if (dialog.isDisplayed())
            dialog.tryClickAt("Закрыть");
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Тестирование в офлайне - Поиск. Кеши скачаны.")
    @TmsLink("navi-mobile-testing-593")  // hash: 0x977f4a6e
    public void Тестирование_в_офлайне_Поиск_Кеши_скачаны() {
        prepare("Скачаны оффлайн кеши региона, в котором производится тестирование. "
            + "На устройстве включен авиарежим. "
            + "Перед прохождением кейса выключить новый поиск: "
            + "Настройки - Dev.Set - Search - Y.Maps search screen - Off "
            + "и Y.Maps search for yandexoid - Off", () -> {
            mapScreen.clickFindMe();
            settings.setOfflineCacheWifiOnly(false);
            downloadCache(Region.VLADIMIR);
            user.setAirplaneMode(true);
        });

        final State state = new State();

        step("Открыть экран 'Поиск' по значку 🔍 в нижней панели. "
                        + "Нажать на любую не-рекламную категорию",
                () -> state.geoCard = mapScreen.clickSearch().clickWhereToEatExpectGeoCard());

        step("Тап по крестику в открывшейся карточке", () -> {
            state.geoCard.closeGeoCard();
            mapScreen.checkSearchIsActive(true);
            expect("Карточка организации закрывается. "
                    + "Отображается плашка 'Результаты без интернета'", () -> {
                user.shouldNotSee(state.geoCard);
                user.shouldSee(mapScreen.offlineResultsPanel);
            });
        });

        step("Отключить авиарежим. Тапнуть по плашке 'Результаты без интернета'", () -> {
            user.setAirplaneMode(false);
            mapScreen.offlineResultsPanel.click();
            GeoCard.getVisible().closeGeoCard();
            expect("Результаты поиска обновляются согласно онлайн-данным.",
                    () -> mapScreen.checkSearchIsActive(true));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({BuildCheck.class, Light.class, UnstableIos.class})
    @DisplayName("Тестирование в офлайне - Поиск по адресу. Кеши скачаны.")
    @TmsLink("navi-mobile-testing-594")  // hash: 0x74b76e8d
    public void Тестирование_в_офлайне_Поиск_по_адресу_Кеши_скачаны() {
        prepare("Скачаны оффлайн кеши региона, в котором производится тестирование. "
            + "На устройстве включен авиарежим. "
            + "Перед прохождением кейса выключить новый поиск: "
            + "Настройки - Dev.Set - Search - Y.Maps search screen - Off "
            + "и Y.Maps search for yandexoid - Off", () -> {
            mapScreen.clickFindMe();
            settings.setOfflineCacheWifiOnly(false);
            downloadCache(Region.VLADIMIR);
            user.setAirplaneMode(true);
        });

        State state = new State();

        step("Перейти на экран Поиск. "
                        + "Ввести любой адрес в поисковую строку. "
                        + "Тапнуть на поисковый саджест",
                () -> state.geoCard =
                        mapScreen.clickSearch().searchAndClickFirstItem("Суздаль Ленина 63А"));

        step("Закрыть карточку организации", () -> {
            state.geoCard.closeGeoCard();
            mapScreen.checkSearchIsActive(true);

            expect("Карточка организации закрывается. "
                    + "Отображается синяя плашка 'Результаты без интернета'", () -> {
                user.shouldNotSee(state.geoCard);
                user.shouldSee(mapScreen.offlineResultsPanel);
            });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Тестирование в офлайне - Добавление точки Избранное. Кеши скачаны.")
    @TmsLink("navi-mobile-testing-595")  // hash: 0xfd85b26c
    public void Тестирование_в_офлайне_Добавление_точки_Избранное_Кеши_скачаны() {
        prepare("Скачаны оффлайн кеши региона, в котором производится тестирование. "
            + "На устройстве включен авиарежим. "
            + "Перед прохождением кейса выключить новый поиск: "
            + "Настройки - Dev.Set - Search - Y.Maps search screen - Off "
            + "и Y.Maps search for yandexoid - Off", () -> {
            mapScreen.clickFindMe();
            settings.setOfflineCacheWifiOnly(false);
            downloadCache(Region.VLADIMIR);
            user.setAirplaneMode(true);
        });

        step("Выполнить лонгтап на карте. "
            + "Нажать на 'В Мои Места'. "
            + "Нажать на строку строку 'Избранное'. "
            + "Ввести любое название, нажать 'Сохранить'.", () ->
            expect("На карте отображается значок с красной книжной закладкой. "
                + "В разделе Мои места отображается строка с названием добавленной точки "
                + "и расстоянием до неё.",
                () -> mapScreen.addBookmarkByLongTap("Моя точка")));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, SkipIos.class})
    @DisplayName("Тестирование в оффлайне - Что здесь? Кеши не скачаны.")
    @TmsLink("navi-mobile-testing-606")  // hash: 0x44e98703
    public void Тестирование_в_оффлайне_Что_здесь_Кеши_не_скачаны() {
        State state = new State();

        prepare("Офлайн кеши не скачаны.", () -> {});

        step("Открыть Яндекс.Навигатор. "
            + "Включить режим 'В самолёте'. Убедиться, что wifi и мобильное соединение выключены. "
            + "Лонгтап по любой точке на карте", () -> {
            user.setAirplaneMode(true);
            state.longTapMenu = mapScreen.longTap();
            expect("Открывается лонгтап меню.", () -> {});
        });

        step("Тап по Что здесь?", () -> {
            state.geoCard = state.longTapMenu.clickWhatIsHere();
            expect("Открывается карточка точки: "
                + "В карточке висит слово 'Загрузка'",
                () -> state.geoCard.checkText("Загрузка"));
        });

        step("Раскрыть карточку точки.", () -> {
            state.geoCard.swipeUp();
            expect("В карточке присутствуют координаты точки",
                () -> state.geoCard.checkText("Координаты"));
        });

        step("Тап по значку Копировать в поле координаты.", () -> {
            state.geoCard.clickCopyCoordinates();
            expect("Координаты скопированы в буфер",
                () -> user.checkClipboard(Pattern.compile("\\d+\\.\\d+, \\d+\\.\\d+")));
        });
    }
}
