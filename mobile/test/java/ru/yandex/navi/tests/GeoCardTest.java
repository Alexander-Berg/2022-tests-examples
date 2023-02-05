package ru.yandex.navi.tests;

import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.AddPlacePopup;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.InputDialog;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.Pin;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class GeoCardTest extends BaseTest {
    private static class State {
        GeoCard geoCard;
        OverviewScreen overviewScreen;
    }

    @Test
    @Ignore("MOBNAVI-23917")
    @Category({UnstableIos.class})
    @TmsLink("navigator-917")  // hash: 0x3513340d
    public void card() {
        checkCard();

        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        checkCard();
    }

    private void checkCard() {
        tabBar.checkVisible();

        GeoCard card = mapScreen.searchAndClickFirstItem("Кулинарная лавка");
        user.shouldSee(card.buttonGo);
        if (user.getOrientation() == ScreenOrientation.PORTRAIT)
            user.shouldNotSee(card.buttonFrom);
        else
            user.shouldSee(card.buttonFrom);

        if (user.getOrientation() == ScreenOrientation.PORTRAIT) {
            card.swipeUp();
            user.shouldSee(card.buttonGo);
            user.shouldSee(card.buttonFrom);
        }

        card.clickGo();
        OverviewScreen.waitForRoute().clickGo();

        card = mapScreen.searchAndClickFirstItem("Хлеб насущный");
        if (user.getOrientation() == ScreenOrientation.PORTRAIT) {
            card.swipeUp();
        }
        user.shouldSee(card.buttonGo);
        user.shouldSee(card.buttonVia);
        card.closeGeoCard();

        // Сбросить маршрут и результаты
        //
        resetRoute();
        if (card.isDisplayed())
            card.closeGeoCard();
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Блок действий в карточке организации")
    @TmsLink("navi-mobile-testing-1539")  // hash: 0xb3f12e8e
    public void Блок_действий_в_карточке_организации() {
        prepareDisableMapsSearch();

        final State state = new State();

        step("Перейти на экран 'Поиск' тапом по иконке Лупы в таббаре. "
            + "Тапнуть на любую категорию", () -> {
            state.geoCard = mapScreen.clickSearch().clickWhereToEatExpectGeoCard();
            expect("При включенном старом поиске: "
                + "Произошел переход на экран карты. "
                + "На карте отображаются пины результатов поиска. "
                + "Снизу плавно выдвинулась карточка организации",
                state.geoCard::checkVisible);
        });

        stepSwipeUpGeoCard(state.geoCard);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Блок действий в карточке организации. Кнопка Позвонить")
    @TmsLink("navi-mobile-testing-522")  // hash: 0xfc594970
    public void Блок_действий_в_карточке_организации_Кнопка_Позвонить() {
        final GeoCard geoCard = prepareGeoCard();

        stepSwipeUpGeoCard(geoCard);

        step("Нажать на 'Позвонить'.", () -> geoCard.clickCall().closeDialActivity());
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Блок действий в карточке организации. Кнопка Сайт")
    @TmsLink("navi-mobile-testing-523")  // hash: 0xdf1a239a
    public void Блок_действий_в_карточке_организации_Кнопка_Сайт() {
        final GeoCard geoCard = prepareGeoCard();

        stepSwipeUpGeoCard(geoCard);

        step("Нажать на 'Сайт'.", () -> {
            if (geoCard.hasButton("Сайт")) {
                geoCard.clickSite();
                user.navigateBack();
            }
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Блок действий в карточке организации. Кнопка Добавить (мои места)")
    @TmsLink("navi-mobile-testing-524")  // hash: 0x6d3f299e
    public void Блок_действий_в_карточке_организации_Кнопка_Добавить_мои_места() {
        final GeoCard geoCard = prepareGeoCard();

        stepSwipeUpGeoCard(geoCard);

        AddPlacePopup addPlacePopup = geoCard.clickAdd();

        final InputDialog saveBookmarkDialog = addPlacePopup.clickFavorites();

        step("Ввести любой текст в окне 'Название закладки'...", () -> {
            saveBookmarkDialog.clickSave();
            geoCard.expectButton("Удалить");
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Отображение кнопок при свайпе карточки")
    @TmsLink("navi-mobile-testing-526")  // hash: 0x850e3b37
    public void Отображение_кнопок_при_свайпе_карточки() {
        final State state = new State();

        prepare(() -> {
            mapScreen.cancelCovidSearch();
            showPointYandex();
            user.waitFor(Duration.ofSeconds(5));
        });

        step("Тапнуть на любую иконку POI на карте", () -> {
            Pin.getPoiPin().tap();
            expect("Снизу плавно выдвинулся контейнер с карточкой организации. "
                + "В Портретной ориентации - Карточка высотой в треть экрана. "
                + "В Альбомной ориентации - Карточка высотой во весь экран. "
                + "В карточке присутствует кнопка 'Поехали'", () -> {
                state.geoCard = GeoCard.getVisible();
                state.geoCard.hasButton("Поехали");
            });
        });

        step("Свернуть карточку в минимальное состояние свайпом вниз", () -> {
            state.geoCard.swipeDownToMinState();
            expect("Карточка отображается в минимальном состоянии - "
                + "отображается 2-3 верхние строки карточки. "
                + "В карточке отсутствует кнопка 'Поехали'", () -> {
                state.geoCard.checkVisible();
                user.shouldNotSee("Поехали");
            });
        });

        stepSwipeUpGeoCard(state.geoCard);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Построение маршрута с промежуточной точкой из карточки организации")
    @TmsLink("navi-mobile-testing-648")  // hash: 0xe9c9dcef
    public void Построение_маршрута_с_промежуточной_точкой_из_карточки_организации() {
        final State state = new State();

        prepare("Построить маршрут любым способом", () -> buildRouteAndGo(ZELENOGRAD));

        step("Перейти на экран 'Поиск' тапом по иконке Лупы в таббаре. "
                + "Тапнуть на любую категорию", () -> {
            state.geoCard = mapScreen.clickSearch().clickWhereToEatExpectGeoCard();
            expect("Произошел переход на экран карты. "
                    + "На карте отображаются пины результатов поиска. "
                    + "Снизу плавно выдвинулась карточка организации",
                    () -> state.geoCard.checkVisible());
        });

        step("Тапнуть на кнопку 'Заехать' в карточке. "
                + "Если девайс в портретной ориентации, раскрыть карточку свайпом вверх.", () -> {
            state.geoCard.swipeUp().clickVia();
            expect("На месте организации устанавливается промежуточная точка. "
                    + "Маршрут перестраивается с учетом промежуточной точки",
                () -> state.overviewScreen = OverviewScreen.waitForRoute());
        });

        step("Тапнуть на 'Поехали' в карточке обзора маршрута", () -> {
            state.overviewScreen.clickGo();
            expect("Происходит переход в режим ведения по маршруту. "
                    + "Камера перемещается к местоположению курсора",
                    () -> mapScreen.checkPanelEta());
        });
    }

    private GeoCard prepareGeoCard() {
        final State state = new State();

        prepare("1 - Пользователь не авторизован. "
                + "2 - Перейти на экран 'Поиск' тапом по иконке Лупы в таббаре. "
                + "3 - Тапнуть на любую категорию",
                () -> state.geoCard = mapScreen.clickSearch().clickWhereToEatExpectGeoCard());

        return state.geoCard;
    }

    @Step("Раскрыть карточку свайпом вверх")
    private void stepSwipeUpGeoCard(GeoCard geoCard) {
        geoCard.swipeUp();

        expect("Карточка раскрывается на всю высоту экрана. "
            + "В карточке отображается блок кнопок. "
            + "Отображаются все или несколько из списка:"
            + "Позвонить/Сайт/Добавить/Поделиться. "
            + "В карточке может отображаться информация об организации: "
            + "Адрес, Телефон, Категории, Режим работы. "
            + "У нижнего края карточки отображаются две кнопки 'Отсюда' и 'Поехали'."
            + "Если включен поиска МЯКа, то в карточке может отображаться кнопка 'Маршрут' "
            + "при тапе на которую будут отображаться кнопки 'сюда' и 'заехать'",
            () -> user.shouldSeeAll("Отсюда", "Поехали"));
    }
}
