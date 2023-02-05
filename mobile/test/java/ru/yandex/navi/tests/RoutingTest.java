package ru.yandex.navi.tests;

import com.google.common.collect.ImmutableList;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.RouteColor;
import ru.yandex.navi.RoutePoint;
import ru.yandex.navi.SearchCategory;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.BookmarksScreen;
import ru.yandex.navi.ui.EditViaPointPopup;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.LongTapMenu;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.Pin;
import ru.yandex.navi.ui.SearchScreen;
import ru.yandex.navi.ui.SearchSuggestScreen;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(RetryRunner.class)
public class RoutingTest extends BaseTest {
    // TODO: we need timeout before clicking pin?
    private static final Duration TIMEOUT_CLICK_PIN = Duration.ofSeconds(5);

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("Автоматизация: Построение и сброс маршрута")
    @TmsLink("navigator-914")  // hash: 0x1bafb320
    public void Автоматизация_Построение_и_сброс_маршрута() {
        class State {
            private LongTapMenu viaPointPopup;
            private OverviewScreen overviewScreen;
        }

        final State state = new State();

        step("Лонгтап на карте в произвольном месте", () -> {
            state.viaPointPopup = mapScreen.longTap();
            expect("Открывается меню", () -> {});
        });

        step("Тап по кнопке 'Сюда' в открывшемся меню", () -> {
            state.viaPointPopup.clickTo();
            expect("Строится маршрут в выбранную точку. "
                + "Произносится голосовое сообщение 'Маршрут построен'", () -> {
                state.overviewScreen = OverviewScreen.waitForRoute();
                user.waitForAnnotations(Duration.ofSeconds(1), "маршрут построен");
            });
        });

        step("Тап по кнопке 'Поехали'", () -> {
            state.overviewScreen.clickGo();
            expect("Переход в режим ведения по маршруту",
                () -> mapScreen.checkPanelEta());
        });

        step("Тап по кнопке 'Обзор' в нижней плашке", () -> {
            state.overviewScreen = mapScreen.clickOverview();
            expect("Открывается экран обзора.", () -> {});
        });

        step("Тап по кнопке 'Сброс'", () -> {
            state.overviewScreen.clickCancel();
            expect("Маршрут удаляется с карты. "
                + "Фокус возвращается к текущему местоположению",
                () -> mapScreen.checkPanelEta(false));
        });
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-14472")
    @Ignore("MOBNAVI-23917")
    public void buildRouteAndAddBookmark() {
        GeoCard geoCard = addBookmarkBySearch();
        geoCard.clickSave().saveToFavorites();

        tabBar.clickMap();

        geoCard = addBookmarkBySearch();
        geoCard.clickGo();

        final OverviewScreen overviewScreen = OverviewScreen.waitForRoute();

        geoCard = overviewScreen.clickSearch().searchAndClickFirstItem("Кафе");
        geoCard.clickSave();  // Won't fix crash: .saveToFavorites();
    }

    private GeoCard addBookmarkBySearch() {
        BookmarksScreen bookmarksScreen = tabBar.clickBookmarks();

        SearchScreen searchScreen = bookmarksScreen.addAddress();
        return searchScreen.searchAndClickFirstItem("Библио-Глобус");
    }

    @Test
    @Category(BuildCheck.class)
    public void buildRouteOnSecondRun() {
        restartAppAndSkipIntro();
        buildRouteToSomePointAndGo();
    }

    @Test
    @Issues({@Issue("MOBNAVI-19834"), @Issue("MOBNAVI-20412")})
    public void buildRouteAndRestartActivity() {
        restartAppAndSkipIntro();
        buildRouteToSomePointAndGo();

        toggleDebugDriving();

        final int N = 5;
        for (int i = 0; i < N; ++i) {
            user.restartActivity();
            user.waitFor(Duration.ofSeconds(5));
            mapScreen.checkRouteColor(RouteColor.ONLINE);
        }
    }

    @Test
    // TODO: changed @TmsLink("navigator-1113")
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-20412")
    public void buildRouteTwice() {
        buildRouteToSomePointAndGo();
        user.restartActivity();
        buildRouteToSomePointAndGo();
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-20286")
    public void buildRouteAndRotate() {
        restartAppAndSkipIntro();

        buildRouteToSomePointAndGo();
        toggleDebugDriving();

        final int N = 5;
        for (int i = 0; i < N; ++i) {
            user.rotatesTo(ScreenOrientation.LANDSCAPE);
            user.waitFor(Duration.ofSeconds(2));
            user.rotatesTo(ScreenOrientation.PORTRAIT);
            user.waitFor(Duration.ofSeconds(2));
        }
    }

    @Test
    @Ignore("MOBNAVI-23917")
    public void goldenRing() {
        mapScreen.buildRouteBySearchAndGo("Москва, Кремль");

        OverviewScreen overviewScreen = null;

        final String[] points = {"Сергиев Посад", "Переславль-Залесский", "Ростов Великий",
                "Ярославль", "Кострома", "Иваново", "Суздаль", "Владимир"};
        for (String point : points) {
            final SearchScreen searchScreen
                = overviewScreen != null ? overviewScreen.clickSearch() : mapScreen.clickSearch();
            searchScreen.searchAndClickFirstItem(point).clickVia();
            overviewScreen = OverviewScreen.waitForRoute();
        }

        OverviewScreen.getVisible().clickGo();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута - кнопкой Сюда")
    @TmsLink("navi-mobile-testing-636")  // hash: 0x3eea70a1
    public void Построение_маршрута_кнопкой_Сюда() {
        stepBuildRouteToSomePoint();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута c промежуточными точками через лонгтап")
    @TmsLink("navigator-1357")  // hash: 0x9ecbd41a
    public void Построение_маршрута_c_промежуточными_точками_через_лонгтап() {
        prepare(() -> {
            experiments.disableMapsSearch().apply();
            prepareCleanMap();
        });

        stepBuildRouteToSomePoint();

        step("Выполнить лонгтап в любую точку на карте и тапнуть по кнопке 'через'.", () -> {
            mapScreen.longTap(0.1, 0.4).clickVia();
            expect("На маршрут добавляется точка.", () -> {
                user.shouldSee(Pin.getViaPin());
                OverviewScreen.waitForRoute();
            });
        });

        step("Выполнить лонгтап по карте. "
            + "Тап на 'Что здесь'. "
            + "В открывшейся карточке нажать на 'Заехать'", () -> {
            mapScreen.longTap(0.7, 0.1).clickWhatIsHere().clickVia();
            expect("На маршрут добавлена промежуточная точка.", () -> {
                OverviewScreen.waitForRoute();
                user.shouldSee(Pin.getViaPin());
            });
        });

        step("Выполнить лонгтап по одной из добавленных на маршрут промежуточных точек "
            + "и перетащить точку в другое место.", () -> {
            user.waitFor(TIMEOUT_CLICK_PIN);
            Pin.getViaPin().longTapAndMoveTo(0.1, 0.8);
            expect("Маршрут перестроен сообразно внесенным изменениям.",
                OverviewScreen::waitForRoute);
        });

        step("Тапнуть по промежуточной точке на маршруте. "
            + "Тап по кнопке 'Удалить'", () -> {
            user.waitFor(TIMEOUT_CLICK_PIN);
            Pin.getViaPin().tap();
            EditViaPointPopup.getVisible().clickRemove();
            expect("Промежуточная точка удалена. "
                + "Маршрут перестроен без учета этой точки",
                OverviewScreen::waitForRoute);
        });
    }

    private void stepBuildRouteToSomePoint() {
        step("Выполнить лонгтап в любую точку карты. "
            + "В появившемся лонгтап-меню нажать на пункт 'Сюда'.", () -> {
            mapScreen.longTap(0.5, 0.3).clickTo();
            expect("Выполняется построение маршрута.",
                OverviewScreen::waitForRoute);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута - кнопкой 'Что здесь?'")
    @TmsLink("navi-mobile-testing-637")  // hash: 0x5a58fec1
    public void Построение_маршрута_кнопкой_Что_здесь() {
        mapScreen.longTap().clickWhatIsHere().clickGo();
        OverviewScreen.waitForRoute();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableAndroid.class, UnstableIos.class})
    @DisplayName("Построение маршрута c промежуточными точками через черный плюс")
    @Ignore("MOBNAVI-20774")
    @TmsLink("navigator-1360")  // hash: 0x33fc677c
    // Открывается поисковое окно. В поисковую строку введен адрес
    public void Построение_маршрута_c_промежуточными_точками_через_черный_плюс() {
        class State {
            private GeoCard geoCard;
            private SearchSuggestScreen searchSuggestScreen;
        }

        State state = new State();

        prepare("1 - Построить маршрут любым способом " +
                "(например, Вызвать лонгтап меню и нажать на пункт 'Сюда'.)", () -> {
            prepareCleanMap();
            buildRoute(VLADIMIR);
        });

        step("Выполнить одиночный тап по черному плюсу на маршруте. "
            + "Не отпуская палец, перетащить синий плюс в другое место.", () -> {
            user.waitFor(TIMEOUT_CLICK_PIN);
            Pin.getAuxPin().longTapAndMoveTo(0.5, 0.6);
            expect("На маршрут добавляется новая промежуточная точка.",
                OverviewScreen::waitForRoute);
        });

        step("Выполнить одиночный тап по черному плюсу на маршруте. "
            + "Тапнуть на 'Уточнить'.", () -> {
            user.waitFor(TIMEOUT_CLICK_PIN);
            Pin.getAuxPin().tap();
            EditViaPointPopup.getVisible().clickAdjust();
            expect("Открывается поисковое окно. " +
                "В поисковую строку введен адрес, " +
                "соответствующий примерному местоположению синего плюса.",
                () -> state.searchSuggestScreen = SearchSuggestScreen.getVisible());
        });

        step("Нажать на любую строку среди отобразившихся поисковых подсказок", () -> {
            state.geoCard = state.searchSuggestScreen.clickFirstItem(2).expectGeoCard();
            expect("Происходит переход на карту. "
                + "Производится поиск по адресу. "
                + "На карте отображается поисковый пин. "
                + "Открыта карточка этой точки.", () -> {
                user.shouldSee(Pin.getSearchPin());
                state.geoCard.checkVisible();
            });
        });

        step("Тап на 'заехать' в открывшейся карточке.", () -> {
            state.geoCard.clickVia();
            expect("На маршрут добавляется новая промежуточная точка.",
                OverviewScreen::waitForRoute);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута через саджест")
    @TmsLink("navi-mobile-testing-638")  // hash: 0x82659975
    public void Построение_маршрута_через_саджест() {
        prepare("Должны отображаться саджесты пунктов назначения", () -> {
            mapScreen.buildRouteBySearchAndGo("Зеленоград");
            resetRoute();
            user.shouldSee(mapScreen.suggestItem);
        });

        mapScreen.clickSuggestItem();
        OverviewScreen.waitForRoute().clickGo();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута - Мои места (Недавние)")
    @TmsLink("navi-mobile-testing-639")  // hash: 0x5df788a6
    public void Построение_маршрута_Мои_места_Недавние() {
        prepare("Для проверки данного кейса должна быть история поездок", () -> {
            buildRouteAndGo(ZELENOGRAD);
            commands.clearRoute();
        });

        final BookmarksScreen bookmarksScreen = mapScreen.clickBookmarks();

        step("Перейти на вкладку 'Недавние'. Тапнуть по любой строке с адресом", () -> {
            bookmarksScreen.clickRecent().clickFirstItem();
            OverviewScreen.waitForRoute();
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута - Мои места (Сохраненное)")
    @TmsLink("navigator-1364")  // hash: 0x7527a4ab
    public void Построение_маршрута_Мои_места_Сохраненное() {
        prepare("Для прохождения кейса, нужно добавить точку в закладки любым способом",
                () -> commands.addBookmark(ZELENOGRAD));

        mapScreen.clickBookmarks().clickItem(ZELENOGRAD.name);
        OverviewScreen.waitForRoute();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Сброс маршрута")
    @TmsLink("navi-mobile-testing-642")  // hash: 0xb3f43113
    public void Сброс_маршрута() {
        step("Построить маршрут любым способом", () -> buildRouteAndGo(ZELENOGRAD));

        step("Тап на кнопку 'Отмена' на карточке обзора маршрута",
                () -> mapScreen.clickOverview().clickCancel());
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-15892")
    @Ignore("MOBNAVI-22940")
    public void moveFinishWhileRouting() {
        GeoCard geoCard = mapScreen.searchAndClickFirstItem("город Магадан");
        geoCard.clickGo();

        Pin.getFinishPin().longTap(Duration.ofSeconds(10));
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Иконка платной дороги в балунах вариантов маршрута")
    @TmsLink("navi-mobile-testing-836")  // hash: 0x46ab17bf
    public void Иконка_платной_дороги_в_балунах_вариантов_маршрута() {
        final GeoPoint NOVOMOSKOVSK = new GeoPoint("Новомосковск", 54.010993, 38.290896);
        final GeoPoint BALASHIKHA = new GeoPoint("Балашиха", 55.796339, 37.938199);

        testAltBalloon(
        "Построить маршрут через платную дорогу (например из Новомосковска в Балашиху)",
            NOVOMOSKOVSK, BALASHIKHA, null,
            "Иконка платной дороги", "rub");
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Иконка плохой дороги в балунах вариантов маршрута")
    @TmsLink("navi-mobile-testing-837")  // hash: 0x4cec8b14
    public void Иконка_плохой_дороги_в_балунах_вариантов_маршрута() {
        final GeoPoint PUSTOSHKA = new GeoPoint("Пустошка", 56.335326, 29.369026);
        final GeoPoint SIMONOVO = new GeoPoint("деревня Симоново", 56.391082, 29.340298);
        final GeoPoint ZAOZERIE = new GeoPoint("деревня Заозерье", 56.408244, 29.144339);

        testAltBalloon(
            "Построить маршрут с плохой дорогой",
            PUSTOSHKA, ZAOZERIE, ImmutableList.of(RoutePoint.via(SIMONOVO)),
            "Иконка плохой дороги", "bad_road");
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Иконка маршрута через границу")
    @TmsLink("navi-mobile-testing-838")  // hash: 0x9b3a7522
    public void Иконка_маршрута_через_границу() {
        final GeoPoint MINSK = new GeoPoint("Минск", 53.902512, 27.561481);

        testAltBalloon(
            "Построить маршрут через границу, например в Минск.",
            null, MINSK, null,
            "Иконка граница", "globe");
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("[2019.09 Сентябрь] Иконки паромной переправы.")
    @TmsLink("navi-mobile-testing-839")  // hash: 0x697e7c97
    public void Иконки_паромной_переправы() {
        final GeoPoint HELSINKI = new GeoPoint("Хельсинки", 60.166892, 24.943673);
        final GeoPoint TALLINN = new GeoPoint("Таллин", 59.421937, 24.743367);

        testAltBalloon(
            "Построить маршрут через переправу.",
            TALLINN, HELSINKI, null,
            "Иконка переправы", "ferry");
    }

    private void testAltBalloon(String description,
                                GeoPoint from, GeoPoint to, List<RoutePoint> via,
                                String iconName, String icon) {
        prepare(this::restartAppAndSkipIntro);  // restart for new Overview screen

        step(description, () -> {
            buildRoute(from, to, via);
            expect(
                iconName + " отображается в балуне соответствующего маршрута, умещается целиком.",
                () -> Pin.getAltBalloonPin(icon));
        });
    }

    @Test
    @Ignore("MOBNAVI-23917")
    @Category({UnstableIos.class})
    @DisplayName("Построение маршрута Отсюда - тап по категории")
    @Issue("MOBNAVI-18930")
    public void Построение_маршрута_Отсюда_тап_по_категории() {
        experiments.disableMapsSearch().applyAndRestart();  // restart to enable new wait cursor

        mapScreen.longTap().clickFrom();

        tabBar.clickSearch().click(SearchCategory.WHERE_TO_EAT);
        GeoCard.getVisible().clickGo();

        OverviewScreen.waitForRoute();
    }

    private void prepareCleanMap() {
        settings.disableRoadEvents();
        experiments.enable(Experiment.NEW_OVERVIEW_SCREEN, Experiment.ROUTE_VARIANT_BALLOONS_OFF)
            .applyAndRestart();
    }
}
