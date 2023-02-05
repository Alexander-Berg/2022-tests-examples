package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.ui.OverviewAdViaPanel;
import ru.yandex.navi.ui.OverviewScreen;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class OverviewAdViaTest extends BaseTest {
    private static final GeoPoint FROM = new GeoPoint("From", 55.725646, 37.580175);
    private static final GeoPoint TO = new GeoPoint("To", 55.740146, 37.577253);

    private static final class State {
        OverviewScreen overviewScreen;
        OverviewAdViaPanel overviewAdViaPanel;
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableAndroid.class, UnstableIos.class})
    @DisplayName("Проверка рекламной сущности Буран")
    @Ignore("MOBNAVI-20207")
    @Issues({@Issue("MOBNAVI-17816"), @Issue("MOBNAVI-18463"), @Issue("MOBNAVI-20207")})
    @TmsLink("navi-mobile-testing-465")  // hash: 0x86434225
    public void Проверка_рекламной_сущности_Буран() {
        prepare();

        final State state = buildRouteWithViaAds();

        switchAdVia(state, true,
            () -> {
                expect("Добавляется виа точка нового перестроенного маршрута. " + "\n" +
                    "Брендированность рекламной виа-точки не теряется " +
                    "(на значке отображается её логотип). " + "\n" +
                    "Артефакты противоположной темы отсутствуют. " +
                    "В логах приходит событие (навигатора) overview-ads.pin-swapped " +
                    "и (мапкит) mapkit.search.logger.billboard.click",
                    () -> {});
        });

        switchAdVia(state, false,
            () -> {
                expect("Маршрут перестроен, он может не совпадать с тем, который был до. " +
                    "Маршрут НЕ проходит через рекламную организацию.",
                    () -> {});
        });

        step("Тапнуть на “Поехали” при включенном буране",
            () -> {
                switchAdVia(state, true);
                state.overviewScreen.clickGo();

                expect("В логи приходит событие " +
                    "(навигатор) route.start-navigation " +
                    "с не пустым параметром: 'overview_ad_campaign_id' " +
                    "и (мапкит) mapkit.search.logger.billboard.navigation.via",
                    () -> {
                        user.shouldSeeLogInAllLogs("route.start-navigation");
                        user.shouldSeeLogInAllLogs("overview_ad_campaign_id");
                        user.shouldSeeLogInAllLogs(
                            "mapkit.search.logger.billboard.navigation.via");
                });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Буран в обзоре при смене ориентации девайса")
    @Issues({@Issue("MOBNAVI-17816"), @Issue("MOBNAVI-18463")})
    @TmsLink("navi-mobile-testing-466")  // hash: 0x16d0ad66
    public void Буран_в_обзоре_при_смене_ориентации_девайса() {
        prepare();

        final State state = buildRouteWithViaAds();
        switchAdVia(state, true,
            () -> {
                expect("Добавляется виа точка нового перестроенного маршрута. " +
                    "Брендированность рекламной виа-точки не теряется " +
                    "(на значке отображается её логотип).",
                    () -> {});
        });

        step("Несколько раз сменить ориентацию девайса",
            () -> {
                rotateAndReturn();
                user.rotates();

                checkAdViaSwitcher(state, true);
                user.rotates();
                expect("Отображение свитчера и промежуточной точки " +
                    "в разных ориентациях одинаковое.",
                    () -> {
                        checkAdViaSwitcher(state, true);
                    });
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Ignore("MOBNAVI-23917")
    @Category({Light.class, UnstableIos.class})
    @DisplayName("Добавление промежуточной точки в маршрут с бураном")
    @Issues({@Issue("MOBNAVI-17816"), @Issue("MOBNAVI-18463"), @Issue("MOBNAVI-19065")})
    @TmsLink("navi-mobile-testing-467")  // hash: 0x1c75b1e3
    public void Добавление_промежуточной_точки_в_маршрут_с_бураном() {
        prepare();

        final State state = buildRouteWithViaAds();

        step("Добавить на маршрут промежуточную точку.",
            () -> {
                state.overviewScreen
                    .clickSearch()
                    .searchAndClickFirstItem("Кремль")
                    .clickVia();
                state.overviewScreen = OverviewScreen.waitForRoute();

                expect("Промежуточная точка установлена." + "\n" +
                    "Буран не отображается на экране обзора",
                    () -> {
                        user.shouldNotSee(state.overviewAdViaPanel);
                });
        });
    }

    private State buildRouteWithViaAds() {
        final State state = new State();

        step("Построить маршрут, проходящий близко к пину рекламного проекта." + "\n" +
                "Маршрут:" + "\n" +
                "iOS: `yandexnavi://build_route_on_map?" +
                "lat_from=55.725646&lon_from=37.580175&lat_to=55.740146&lon_to=37.577253`" + "\n" +
                "Android: `adb shell am start -a android.intent.action.VIEW -d " +
                "'yandexnavi://build_route_on_map?" +
                "lat_from=55.725646&lon_from=37.580175&lat_to=55.740146&lon_to=37.577253'`",
            () -> {
                state.overviewScreen = buildRoute(FROM, TO, null);

                expect("В экране обзора появляется свитчер с названием рекламной организации. " +
                    "Артефакты противоположной темы отсутствуют. " + "\n" +
                    "В логи приходит событие " +
                    "(навигатора) overview-ads.pin-shown" + "\n" +
                    "В логи приходит событие " +
                    "(мапкит) mapkit.search.logger.billboard.show" + "\n",
                    () -> {
                        user.waitForLog("overview-ads.pin-shown", Duration.ofSeconds(10));
                        user.waitForLog("mapkit.search.logger.billboard.show",
                            Duration.ofSeconds(10));
                        state.overviewAdViaPanel = OverviewAdViaPanel.getVisible();
                        final String title = state.overviewAdViaPanel.getTitle();
                        Assert.assertTrue("Can't find via title!",
                            title != null && !title.isEmpty());
                });
        });

        return state;
    }

    private void switchAdVia(State state, boolean shouldBeEnabled, Runnable runnable) {
        final String status = shouldBeEnabled ? "вкл." : "выкл.";
        step("Переключить свитчер в положение " + status,
            () -> {
                state.overviewAdViaPanel.clickSwitcher();
                checkAdViaSwitcher(state, shouldBeEnabled);
                runnable.run();
        });
    }

    private void switchAdVia(State state, boolean shouldBeEnabled) {
        switchAdVia(state, shouldBeEnabled, () -> {});
    }

    private void checkAdViaSwitcher(State state, boolean shouldBeEnabled) {
        final String statusRu = shouldBeEnabled ? "вкл." : "выкл.";
        final String statusEn = shouldBeEnabled ? "on." : "off.";
        expect("Свитчер находится в положении " + statusRu,
            () -> {
                Assert.assertEquals("Via switcher must be " + statusEn,
                    shouldBeEnabled,
                    state.overviewAdViaPanel.isSelected());
        });
    }

    private void prepare() {
        prepare("Для проверки отображения сущности в нужной теме, " +
            "следует включить Настройку в " +
            "Developer settings ->Misc ->Fast day and night switching = on. " +
            "Перезагрузить Навигатор", () -> {
            enableForceDatatesting();
            experiments.disableMapsSearch()
                .enable(Experiment.DAY_NIGHT_FAST_SWITCH, Experiment.IGNORE_ADS_COOLDOWN)
                .disable(Experiment.DISABLE_OVERVIEW_ADS, Experiment.NEW_OVERVIEW_SCREEN_TABS,
                    Experiment.TAXI_AD_ROUTE_AT_SECOND_POSITION,
                    Experiment.TAXI_AD_ROUTE_AT_THIRD_POSITION)
                .set(Experiment.OVERIVEW_ADS_PRIORITY, "via")
                .applyOnly();
            restartAppAndSkipIntro();
        });
    }
}
