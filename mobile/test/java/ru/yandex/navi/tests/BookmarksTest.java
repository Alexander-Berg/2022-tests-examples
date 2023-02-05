package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.BookmarksScreen;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.Pin;
import ru.yandex.navi.ui.SearchScreen;

@RunWith(RetryRunner.class)
public final class BookmarksTest extends BaseTest {
    private static final String MY_LIST = "Мой список";
    private static final String MY_POINT_1 = "Моя точка #1";
    private static final String MY_POINT_2 = "Моя точка #2";

    private static class State {
        BookmarksScreen bookmarksScreen;
        GeoCard geoCard;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("'Мои места'. Добавление Дом/Работа")
    @Issue("MOBNAVI-11211")
    @TmsLink("navi-mobile-testing-1058")  // hash: 0x63215685
    public void Мои_места_Добавление_ДомРабота() {
        final State state = new State();

        prepare("1 - Перед прохождением кейса выключить новый поиск: "
            + "Настройки - Dev.Set - Search - Y.Maps search screen - Off. "
            + "Y.Maps search for yandexoid - Off. "
            + "Перезагрузить приложение. "
            + "2 - Точки Дом/Работа не добавлены",
            () -> experiments.disableMapsSearch().applyAndRestart());

        step("Добавить специальные точки 'Дом' или 'Работа'. "
            + "Для этого удерживать палец на карте до появления меню сверху, "
            + "нажать на 'В Мои Места', нажать на строку 'Дом'/'Работа'.", () -> {
            mapScreen.longTap().clickMyPlaces().clickHome();
            expect("На карте отображается значок с красным домиком / с красным портфелем",
                Pin::getHomePin);
        });

        step("Нажать на кнопку 'Мои места' (кнопка в виде двух закладок внизу экрана)", () -> {
            state.bookmarksScreen = tabBar.clickBookmarks();
            expect("Если пользователь неавторизован: "
                + "В разделе 'Мои места' напротив строки 'Дом' пропадает текст 'добавить'."
                + "Адрес добавленной не отображается. "
                + "На iOS: Отображается расстояние до добавленной точки. "
                + "Если пользователь авторизован: "
                + "В разделе 'Мои места' напротив строки 'Дом' пропадает текст 'добавить'. "
                + "Отображается адрес добавленной точки. "
                + "На iOS: Отображается расстояние до добавленной точки. "
                + "(Адрес может подтягиваться в течении нескольких секунд)",
                () -> state.bookmarksScreen.checkButtonAdd("Дом", false));
        });

        step("Нажать на строку 'Дом'.", () -> {
            state.bookmarksScreen.clickHomeExpectOverview();
            expect("Производится возврат к экрану карты. Строится маршрут до точки 'Дом'",
                () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("'Мои места'. Удаление точек Дом/Работа")
    @TmsLink("navi-mobile-testing-1059")  // hash: 0x16b39339
    public void Мои_места_Удаление_точек_ДомРабота() {
        final State state = new State();

        prepare("У пользователя добавлены точки Дом/Работа в Моих местах. "
            + "Если таковых нет, то необходимо предварительно добавить. "
            + "Для этого удерживать палец на карте до появления меню сверху, "
            + "нажать на 'В Мои Места', нажать на строку 'Дом'.", this::addPointHome);

        step("Нажать на кнопку 'Мои места' (кнопка в виде двух закладок внизу экрана)", () -> {
            state.bookmarksScreen = tabBar.clickBookmarks();
            expect("В разделе 'Мои места' напротив строки 'Дом'/'Работа'. "
                + "Отображается расстояние до добавленной точки ( только для IOS )",
                () -> state.bookmarksScreen.checkVisible());
        });

        step("Нажать на значок карандаша вверху экрана. "
            + "Нажать на строку 'Дом'. "
            + "Нажать на значок корзины. "
            + "Подтвердить удаление точки 'Дом'.", () -> {
            state.bookmarksScreen.clickEdit().clickCheckbox("Дом").clickRemove();
            expect("Точка удаляется. "
                + "В строчке 'Дом' снова отображается слово 'Добавить'."
                + "Расстояние не отображается.", () ->
                state.bookmarksScreen.checkButtonAdd("Дом", true));
        });

        step("Перейти на экран карты, закрыв раздел Мои места", () -> {
            tabBar.clickMap();
            expect("На карте пропадает значок с домом.", () -> Pin.checkPinHome(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Мои места. Добавление точек в блок 'Избранное'")
    @TmsLink("navi-mobile-testing-1061")  // hash: 0x35e289b4
    public void Мои_места_Добавление_точек_в_блок_Избранное() {
        step("Добавить точку в блок 'ИЗБРАННОЕ'. "
            + "Для этого удерживать палец на карте до появления меню сверху, "
            + "нажать на 'В Мои Места', нажать на строку 'Избранное', ввести любое название, "
            + "нажать 'Сохранить'. "
            + "Открыть Мои места, нажать на строку с названием добавленной точки.", () -> {
            mapScreen.addBookmarkByLongTap(MY_POINT_1);
            expect("На карте отображается красный значок с книжной закладкой. "
                + "Также в меню Мои места отображается строка с названием добавленной точки "
                + "и расстоянием до неё.", () -> {});
        });

        step("Открыть 'Мои места' (кнопка в виде двух закладок внизу экрана). "
            + "Нажать на строку с названием добавленной точки.", () -> {
            tabBar.clickBookmarks().clickItem(MY_POINT_1);
            expect("Производится возврат к экрану карты. "
                + "Строится маршрут до выбранной точки.", OverviewScreen::waitForRoute);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Мои места. Удаление точек из блока 'Избранное'")
    @TmsLink("navi-mobile-testing-1062")  // hash: 0x050956e7
    public void Мои_места_Удаление_точек_из_блока_Избранное() {
        prepare("Добавлены точки в блок 'Избранное' в Моих местах",
                () -> mapScreen.addBookmarkByLongTap(MY_POINT_1));

        State state = openBookmarks();

        step("Нажать на значок карандаша вверху экрана. "
            + "Нажать на строку удаляемой точки в блоке 'Избранное'. "
            + "Нажать на значок корзины. "
            + "Подтвердить удаление точки из Избранного. "
            + "Нажать на 'Готово' для завершения процесса удаления", () -> {

            state.bookmarksScreen.clickEdit().clickCheckbox(MY_POINT_1).clickRemove();
            tabBar.clickMap();

            expect("Точка удаляется. "
                + "Точка больше не отображается на карте, "
                + "в меню 'Мои Места' название точки отсутствует.", () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Мои места. Создание списков и добавление точек в них")
    @TmsLink("navi-mobile-testing-1063")  // hash: 0xbb243193
    public void Мои_места_Создание_списков_и_добавление_точек_в_них() {
        createList();
        addPointToList();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута - Избранное")
    @TmsLink("navi-mobile-testing-641")  // hash: 0x44d165ad
    public void Построение_маршрута_Избранное() {
        prepare("Для прохождения кейса, нужно добавить точки в закладки любым способом",
            () -> {
                dismissPromoBanners();
                addPointHome();
            });

        State state = new State();

        step("Тапнуть по любой точке Избранного на карте", () -> {
            Pin.getHomePin().tap();
            expect("Открывается карточка Избранного",
                () -> state.geoCard = GeoCard.getVisible());
        });

        step("Тапнуть по кнопке 'Поехали'", () -> {
            state.geoCard.clickGo();
            expect("Строится маршрут до выбранной точки",
                OverviewScreen::waitForRoute);
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Мои места. Удаление точек из списка")
    @TmsLink("navi-mobile-testing-1064")  // hash: 0x12b071f2
    public void Мои_места_Удаление_точек_из_списка() {
        prepare("У пользователя добавлены точки в Списки в Моих местах. "
            + "Если таковых нет, то необходимо предварительно добавить. "
            + "Для этого удерживать палец на нужной точке на карте до появления меню сверху, "
            + "нажать на 'В Мои Места', нажать на строку с созданным списком, "
            + "ввести любое название, нажать 'Сохранить'.", () -> {
            createList();
            addPointToList();
        });

        final State state = openBookmarks();

        step("Нажать на название любого списка. "
            + "Перейти в режим редактирования тапом по иконке карандаша. "
            + "Выбрать любую из точек и удалить ее.", () -> {
            state.bookmarksScreen.clickItem(MY_LIST).clickEditExpect(MY_LIST)
                .clickCheckbox(MY_POINT_2).clickRemove();
            expect("Точка удалена из списка.", () -> user.shouldNotSee(MY_POINT_2));
        });
    }

    private State openBookmarks() {
        State state = new State();

        step("Нажать на кнопку 'Мои места' (кнопка в виде двух закладок внизу экрана)", () -> {
            state.bookmarksScreen = tabBar.clickBookmarks();
            expect("Производится переход в раздел 'Мои места'. "
                + "Отображается список добавленных в Избранное точек.", () -> {});
        });

        return state;
    }

    private void addPointHome() {
        mapScreen.longTap().clickMyPlaces().clickHome();
    }

    @Step("Создать список")
    private void createList() {
        tabBar.clickBookmarks().clickNewList().enterText(MY_LIST).clickSave();
        tabBar.clickMap();
    }

    @Step("Добавить точку в список")
    private void addPointToList() {
        mapScreen.longTap().clickMyPlaces().clickItem(MY_LIST).enterText(MY_POINT_2).clickSave();
    }

    @Test
    @Ignore("MOBNAVI-23917")
    public void addBookmarkBySearch() {
        BookmarksScreen bookmarksScreen = tabBar.clickBookmarks();
        rotateAndReturn();

        SearchScreen searchScreen = bookmarksScreen.addAddress();
        rotateAndReturn();

        searchScreen.searchAndClickFirstItem("Зеленоград");

        mapScreen.longTap().clickMyPlacesExpectAddBookmark().clickSave();
    }

    @Test
    @Category({UnstableIos.class})
    @Ignore("MOBNAVI-12712")
    public void movePinWhatIsHere() {
        mapScreen.longTap().clickWhatIsHere();
        Pin.getWhatIsHerePin().longTapAndMoveTo(0.8, 0.5);
    }
}
