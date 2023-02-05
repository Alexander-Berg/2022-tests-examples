package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.Pin;
import ru.yandex.navi.ui.SearchHistoryScreen;
import ru.yandex.navi.ui.SearchSuggestScreen;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class SearchTest extends BaseTest {
    @Test
    @Ignore("MOBNAVI-23917")
    // TODO: changed @TmsLink("navigator-849")
    public void search() {
        mapScreen.clickSearch().clickWhereToEatExpectGeoCard().closeGeoCard();
    }

    @Test
    @Ignore("MOBNAVI-23917")
    @Category({BuildCheck.class})
    // TODO: changed @TmsLink("navigator-849")
    @Issue("MOBNAVI-22062")
    public void suggest() {
        final String[] queries = {"Азбука вкуса", "Германия, Берлин", "Германия Берлин"};
        for (String query : queries) {
            mapScreen.clickSearch().searchFor(query)
                .clickFirstItem().expectGeoCard().closeGeoCard();
        }
    }

    private static class State {
        SearchSuggestScreen searchSuggestScreen;
        SearchHistoryScreen searchHistoryScreen;
        GeoCard geoCard;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск по категориям")
    @TmsLink("navi-mobile-testing-1517")  // hash: 0xc69cf2ce
    public void Поиск_по_категориям() {
        final State state = new State();

        prepareDisableMapsSearch(() -> {
            dismissPromoBanners();

            // Выключить слой пробок, чтобы дороги были окрашены одинаково.
            // Это упрощает тап по карте mapScreen.tapMap().
            //
            mapScreen.tapTrafficButton();
        });

        step("Перейти на экран 'Поиск' тапом по иконке Лупы в таббаре. "
            + "Тапнуть на любую категорию", () -> {
            state.geoCard = tabBar.clickSearch().clickWhereToEatExpectGeoCard();
            expect("Произошел переход на экран карты. "
                + "На карте отображаются пины результатов поиска. "
                + "Пин организации больше остальных. "
                + "Снизу плавно выдвинулась карточка организации", () -> {
                Pin.getPoiPin();
                state.geoCard.checkVisible();
            });
        });

        step("Скроллить карту в разных направлениях, "
            + "не нажимая при этом на другие пины результатов поиска", () -> {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.NONE)
                    continue;
                mapScreen.swipe(direction);
                user.waitFor(Duration.ofSeconds(1));
                mapScreen.swipe(direction);
                user.waitFor(Duration.ofSeconds(1));
            }
            expect("По мере сдвига карты показываются новые результата поиска"
                + "Карточка организации не скрывается", () -> {
                Pin.getPoiPin();
                state.geoCard.checkVisible();
            });
        });

        step("Тапнуть по карте.", () -> {
            mapScreen.tapMap();
            expect("Карточка скрывается. Пины результатов поиска не удаляются с карты.",
                () -> user.shouldNotSee(state.geoCard));
        });

        step("Тапнуть на любой пин", () -> {
            Pin.getPoiPin().tap();
            expect("Пин, на который произвелось нажатие, выделяется. "
                + "Снизу выдвигается карточка с названием организации, "
                + "ее адресом и кнопкой 'Поехали'", () -> {
                state.geoCard = GeoCard.getVisible();
                state.geoCard.expectButton("Поехали");
            });
        });

        step("Закрыть карточку результата поиска. "
            + "Нажать на кнопку сброса результатов  (кнопка с символом Х)", () -> {
            state.geoCard.closeGeoCard();
            mapScreen.cancelSearch();
            expect("Результаты поиска исчезли с карты",
                () -> mapScreen.checkSearchIsActive(false));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск по введенной категории")
    @TmsLink("navi-mobile-testing-1518")  // hash: 0x0cf7e12d
    public void Поиск_по_введенной_категории() {
        prepare("Выключить следующие настройки: "
            + "Dev Settings > Search > Y.Maps search foe yandexoid > Off. "
            + "Dev Settings > Search > Y.Maps search screen > Off", () -> {});

        final State state = stepOpenSearch();

        step("Начать вводить 'магазин'. "
                + "Выбрать пункт из предложенных с большим количеством результатов", () -> {
            state.geoCard = state.searchSuggestScreen.typeText("магазин")
                .clickFirstItem().expectGeoCard();
            expect("Происходит переход на экран 'Карта'. "
                + "На карте отображаются результаты поиска. "
                + "Снизу выдвигается карточка результата поиска, его адресом и кнопкой 'Поехали'",
                () -> state.geoCard.hasButton("Поехали"));
        });

        step("Закрыть карточку результата поиска. "
                + "Нажать на кнопку сброса результатов (кнопка с символом Х)", () -> {
            state.geoCard.closeGeoCard();
            mapScreen.cancelSearch();
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск из истории")
    @TmsLink("navi-mobile-testing-1519")  // hash: 0x7b7201a6
    public void Поиск_из_истории() {
        final State state = new State();

        prepare("В истории поиска должны быть результаты "
                        + "(предварительно должен быть осуществлен поиск по категории или адресу)",
                () -> {
                    mapScreen.searchAndClickFirstItem("Хлеб насущный").closeGeoCard();
                    mapScreen.cancelSearch();
                });

        step("Открыть экран 'Поиск' по значку Лупы в таббаре. "
                    + "Свайпом влево или тапом по вкладке 'История' перейти на вкладку 'История'",
                () -> {
                    state.searchHistoryScreen = mapScreen.clickSearch().clickHistory();
                    expect("Отображается список выполненных ранее поисковых запросов",
                            () -> user.shouldSeeSuggest(state.searchHistoryScreen.items));
                });

        step("Тап на любую строку, содержащуюся в истории поиска.", () -> {
            state.geoCard = state.searchHistoryScreen.clickFirstItem().expectGeoCard();
            expect("Происходит переход на экран 'Карта'. "
                + "На карте отображаются результат поиска, "
                + "соответствующий запросу из вкладки 'История'. "
                + "Снизу выдвигается карточка результата поиска, его адресом и кнопкой 'Поехали'",
                () -> {
                    state.geoCard.checkVisible();
                    state.geoCard.expectButton("Поехали");
            });
        });

        stepCloseSearch(state);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск по введенному адресу")
    @TmsLink("navi-mobile-testing-1542")  // hash: 0x5028c4c1
    public void Поиск_по_введенному_адресу() {
        prepareDisableMapsSearch();

        final State state = stepOpenSearch();

        step("Ввести в поисковую строку любой адрес с улицей и домом. "
            + "Тап на поисковый саджест (подсказку под поисковой строкой). "
            + "Внимание:может понадобиться дополнительный тап по саджесту, "
            + "если производится поиск по названию улицы без указания номера дома", () -> {
            state.geoCard = state.searchSuggestScreen.typeText("Волхонка 10")
                .clickFirstItem().expectGeoCard();

            expect("Происходит переход на экран 'Карта'. "
                + "На карте отображаются результаты поиска. "
                + "На карте отображается пин. "
                + "Снизу открывается карточка результата поиска в среднем состоянии, "
                + "его адресом и кнопкой 'Поехали'", () -> {
                user.waitFor(Duration.ofSeconds(1));  // TODO: add wait to getSearchPin
                Pin.getSearchPin();
                state.geoCard.hasButton("Поехали");
            });
        });

        step("Зумить и двигать карту двумя пальцами на экране (не делая тап по карте)", () -> {
            mapScreen.swipe(Direction.LEFT).swipe(Direction.RIGHT);
            Pin.getSearchPin();

            mapScreen.rotate();
            Pin.getSearchPin();

            expect("Отображение пина не меняется. "
                + "Пин не пропадает. "
                + "Карточка результата поиска не пропадает.", () -> {
                Pin.getSearchPin();
                state.geoCard.checkVisible();
            });
        });

        stepCloseSearch(state);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск по несуществующему адресу")
    @TmsLink("navi-mobile-testing-1543")  // hash: 0x9673c1c3
    public void Поиск_по_несуществующему_адресу() {
        final State state = stepOpenSearch();

        step("Ввести в поисковую строку любой несуществующий адрес", () -> {
            state.searchSuggestScreen.typeText("КукуКукуКуку, 13");

            expect("Навигатор выдает сообщение: 'Ничего не найдено' "
                + "или ищет объект с похожим названием.", () ->
                state.searchSuggestScreen.expectError("Ничего не найдено", Duration.ofSeconds(1)));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Поиск по введенному адресу улицы")
    @TmsLink("navi-mobile-testing-900")  // hash: 0xdbe17e1b
    public void Поиск_по_введенному_адресу_улицы() {
        final State state = stepOpenSearch();

        step("Ввести в поисковую строку любую улицу (без номера дома!). "
            + "Тап на поисковый саджест (подсказку под поисковой строкой)"
            + "Может потребоваться повторный тап на саджест либо кнопку 'найти'", () -> {
            state.geoCard = state.searchSuggestScreen.typeText("Пречистенка")
                .clickFirstItem(2).expectGeoCard();
            expect("Происходит переход на экран 'Карта'. "
                + "На карте отображаются результаты поиска: "
                + "пин и сама улица выделена темным цветом в дневной теме, "
                + "светлым цветом в ночной теме. "
                + "Снизу выдвигается карточка результата поиска, его адресом "
                + "и кнопкой 'Поехали' (для старого меню) / "
                + "кнопками Маршрут. Поделиться, В избранное (для нового меню от МЯКа)", () -> {
                state.geoCard.checkVisible();
                state.geoCard.hasButton("Поехали");
            });
        });

        stepCloseSearch(state);
    }

    @Step("Перейти на экран 'Поиск' тапом по иконке Лупы в таббаре. Тап на поле ввода 'Поиск'")
    private State stepOpenSearch() {
        final State state = new State();
        state.searchSuggestScreen = mapScreen.clickSearch().clickSearch();

        expect("Курсор установлен на поисковую строку. "
            + "Появилась клавиатура. "
            + "Произошел автоматический переход на вкладку 'История'.", () -> {});

        return state;
    }

    @Step("Закрыть карточку результата поиска. "
        + "Нажать на кнопку сброса результатов (кнопка с символом Х)")
    private void stepCloseSearch(State state) {
        state.geoCard.closeGeoCard();
        mapScreen.cancelSearch();
        expect("Результаты поиска исчезли с карты",
            () -> mapScreen.checkSearchIsActive(false));
    }

    @Test
    @Issue("MOBNAVI-15513")
    @Ignore("MOBNAVI-23917")
    public void searchReturnsMeridian180() {
        mapScreen.searchAndClickFirstItem("Чукотский автономный округ");
    }
}
