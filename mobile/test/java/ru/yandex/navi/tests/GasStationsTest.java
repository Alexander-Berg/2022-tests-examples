package ru.yandex.navi.tests;

import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.SearchCategory;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.GasStationCard;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.LoginScreen;
import ru.yandex.navi.ui.OverviewScreen;

import java.time.Duration;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(RetryRunner.class)
public final class GasStationsTest extends BaseTest {
    private static final GeoPoint LOBNOYE_MESTO
        = new GeoPoint("Лобное место", 55.753229, 37.622503);

    private static final class State {
        GasStationCard gasStationCard;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableAndroid.class, UnstableIos.class})
    @DisplayName("[Заправки] Восстановление сессии оплаты")
    @Ignore("YCARHEADUNIT-10465")
    @TmsLink("navi-mobile-testing-830")  // hash: 0x0cd3dfc8
    // GasStationCard disappears
    // TODO: login, payment
    public void Восстановление_сессии_оплаты() {
        final State state = new State();

        prepare("1 - Пользователь авторизован. "
            + "2 - Включены тестовые заправки и заправочный слой: "
            + "Меню - Настройки - Developer settings - Gas Stations - Switch to Test Mode. "
            + "Меню - Настройки - Developer settings - Gas Stations "
            + "- Switch TankerSDK to test mode on app start. "
            + "Меню - Настройки - Developer settings - Gas Stations - Gas Stations layer. "
            + "Перезагрузить приложение. "
            + "3 - Местоположение пользователя определяется на территории полигона АЗС с оплатой "
            + "(тестировочная АЗС- Москва, Красной площади, Лобное место). "
            + "4 - Открыта карточка заправки Тестировочной АЗС", () -> {
            user.setLocation(LOBNOYE_MESTO);
            tabBar.clickMenu().clickSettings().click("Developer settings", "Gas stations",
                "Switch to Test Mode");
            experiments
                .enable(Experiment.GAS_STATIONS_LAYER, Experiment.GAS_STATIONS_FORCE_TEST_MODE)
                .applyAndRestart();
            state.gasStationCard = GasStationCard.getVisible();
        });

        step("Выбрать любую колонку в карточке заправки", () -> {
            state.gasStationCard.clickColumn().skipFirstOrder();
            expect("Происходит переход к разделу выбора вида топлива, "
                + "способа оплаты и суммы/объема заправки", () -> {});
        });

        step("Выбрать любой тип топлива и объем/сумму заправки. "
            + "Тапнуть на кнопку 'Оплатить'", () -> {
            state.gasStationCard.click("95");
            // TODO: выбрать объем/сумму заправки
            state.gasStationCard.clickPay();
            // TODO:
            expect("Происходит переход к процессу отправки заказа", LoginScreen::getVisible);
        });

        step("Дождаться начала этапа заливки топлива (счетчика литров). "
            + "Выгрузить приложение из мультизадачности. "
            + "Снова запустить приложение", () -> {
            restartAppAndSkipIntro();
            expect("Приложение запускается. "
                + "Открывается карточка Заправки на этапе заливки топлива",
                () -> user.shouldSee(state.gasStationCard));
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class, UnstableIos.class})
    @DisplayName("[COVID-19][Заправки] Закрытие заправочного слоя при поиске")
    @Ignore("MOBNAVI-21093")
    @TmsLink("navi-mobile-testing-775")  // hash: 0x00000000
    // no GeoCard
    public void Закрытие_заправочного_слоя_при_поиске() {
        prepareGasStationLayer();

        step("Перейти в меню поиска по категории. "
            + "Тап по пункту Яндекс.Заправки", () -> {
            mapScreen.clickSearch().click(SearchCategory.YANDEX_GAS_STATIONS);
            GeoCard.getVisible().closeGeoCard();
            mapScreen.zoomOut(4);
            user.waitFor(Duration.ofSeconds(1));
            expect("Показан слой с пинами заправок, в которых есть оплата из навигатора",
                () -> mapScreen.checkGasStationsLayer(true));
        });

        step("Произвести поиск по любой категории через пункт меню Поиск", () -> {
            mapScreen.clickSearch().click(SearchCategory.WHERE_TO_EAT);
            expect("Заправочный слой выключается. "
                    + "На карте отображаются результаты поиска",
                () -> {
                    mapScreen.checkGasStationsLayer(false);
                    GeoCard.getVisible();
                });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableAndroid.class, UnstableIos.class})
    @DisplayName("[COVID-19][Заправки] Закрытие заправочного слоя при ведении по маршруту")
    @Ignore("MOBNAVI-20598")
    @TmsLink("navi-mobile-testing-782")  // hash: 0xe2971961
    // MOBNAVI-18676
    public void Закрытие_заправочного_слоя_при_ведении_по_маршруту() {
        prepareGasStationLayer();

        step("Включить заправочный слой тапом по пункту Яндекс.Заправки. "
            + "Произвести построение маршрута", () -> {
            mapScreen.clickSearch().click(SearchCategory.YANDEX_GAS_STATIONS);
            buildRoute();
            expect("Заправочный слой отображается вместе с построенным маршрутом. "
                + "Также отображается экран обзора маршрута с вариантами маршрута и иконкой 'Лупа' "
                + "и кнопками Отмена и Поехали",
                () -> mapScreen.checkGasStationsLayer(true));
        });

        step("Тап на Поехали", () -> {
            OverviewScreen.getVisible().clickGo();
            expect("Прооисходит переход на карту в режим ведения. "
                + "Заправочный слой выключен", () -> mapScreen.checkGasStationsLayer(false));
        });
    }

    @Step("1 - Внимание!. "
        + "Данный кейс НЕВОЗМОЖНО пройти без подмены местоположения. "
        + "Для проверки нужно подменить местоположение девайса на любую точку в  Москве. "
        + "2 - Включить настройку в Developer Settings -> Gas stations -> Gas stations layer = on. "
        + "3 - Перезапустить приложение.")
    private void prepareGasStationLayer() {
        experiments.disableMapsSearch().enable(Experiment.GAS_STATIONS_LAYER).applyAndRestart();
    }
}
