package ru.yandex.navi.tests;

import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.SearchScreen;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class MapsSearchTest extends BaseTest {
    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class, UnstableAndroid.class})
    @Ignore("MOBNAVI-22063")
    @DisplayName("[Поиск от МЯК] Категории поиска")
    @TmsLink("navi-mobile-testing-876")  // hash: 0x79063dba
    public void Категории_поиска() {
        user.setLocation(YANDEX);  // for "Yandex Gas Stations" search category
        user.waitFor(Duration.ofSeconds(5));  // for "Yandex Gas Stations" search category

        prepareMapsSearch();
        restartAppAndSkipIntro();  // TODO: additional restart for "Yandex Gas Stations"

        step("Тап на поиск в таббаре.", () -> {
            SearchScreen searchScreen = mapScreen.clickSearch();
            expect("Открылся экран поиска. "
                + "Вверху экрана: "
                + "Кнопка вызова голосового ввода в виде микрофона. "
                + "Поле ввода с мигающим курсором. "
                + "Кнопка закрытия в виде крестика. "
                + "На 4.09 - кнопки отключены. Ждут фидбек от пользователей "
                + "(инфа в прилинкованном тикете 18150) : "
                + "Ниже две кнопки переключающие вкладки 'Категории' и 'История'. "
                + "Дальше идет список поисковых категорий: "
                + "Яндекс заправки. "
                + "АЗС. "
                + "Где поесть. "
                + "Продукты. "
                + "Банкоматы. "
                + "Аптеки. "
                + "Гостиницы. "
                + "Автомойки. "
                + "Автосервисы. "
                + "Больницы. "
                + "Кинотеатры. "
                + "Бары. "
                + "ТЦ. "
                + "(порядок категорий может отличаться, "
                + "необходимо проверить только наличие категорий!). "
                + "Ниже размещены рекламные категории: "
                + "Бургер Кинг (рекламная категория, может не присутствовать). "
                + "Макдоналдс (рекламная категория, может не присутствовать). "
                + "KFC (рекламная категория, может не присутствовать). "
                + "Новостройки (рекламная категория, может не присутствовать)",
                () -> searchScreen.checkCategories(
                    "Яндекс Заправки", "АЗС", "Где поесть", "Продукты",
                    "Банкоматы", "Аптеки",
                    "Гостиницы", "Автомойки", "Автосервисы",
                    "Больницы", "Кинотеатры", "Бары" /* TODO: optional: "ТЦ" */));
        });
    }

    private void prepareMapsSearch() {
        prepare("Включить поиск МЯКа: "
            + "Developer settings в разделе Search - Yandex Maps search screen - On",
            () -> experiments.enable(Experiment.MAPS_SEARCH).applyAndRestart());
    }
}
