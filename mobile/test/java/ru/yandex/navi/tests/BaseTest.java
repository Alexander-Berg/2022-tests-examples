package ru.yandex.navi.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.navi.Commands;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.NaviTheme;
import ru.yandex.navi.RoutePoint;
import ru.yandex.navi.Settings;
import ru.yandex.navi.Sounds;
import ru.yandex.navi.tf.LogManager;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.MobileUserListener;
import ru.yandex.navi.tf.NoRetryException;
import ru.yandex.navi.tf.Platform;
import ru.yandex.navi.tf.UserCapabilities;
import ru.yandex.navi.ui.CrashDialog;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.IntroScreen;
import ru.yandex.navi.ui.MapPromoBanner;
import ru.yandex.navi.ui.MapScreen;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.PromoBanner;
import ru.yandex.navi.ui.SearchlibScreen;
import ru.yandex.navi.ui.TabBar;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BaseTest implements MobileUserListener {
    final UserCapabilities userCaps = new UserCapabilities();
    MobileUser user;

    Settings settings;
    MapScreen mapScreen;
    TabBar tabBar;
    Commands commands;
    Experiments experiments;

    static final GeoPoint VLADIMIR = new GeoPoint("Владимир", 56.129057, 40.406635);
    static final GeoPoint SMOLENSKAYA = new GeoPoint("метро Смоленская", 55.747900, 37.582718);
    static final GeoPoint YANDEX = GeoPoint.YANDEX;
    static final GeoPoint YAROSLAVL = new GeoPoint("Ярославль", 57.626578, 39.893858);
    static final GeoPoint ZELENOGRAD = new GeoPoint( "Зеленоград", 55.991893, 37.214382);

    @Before
    public void setUp() {
        user = MobileUser.create(userCaps, this);
        user.setSoundDecoder("audio.play:", new Sounds());

        waitForStartup();
        doBeforeStart();
        doSkipIntro();
    }

    @Override
    public void onDriverCreated(MobileUser user) {
        tabBar = new TabBar();
        mapScreen = new MapScreen();
        commands = new Commands(user);
        settings = new Settings(user);
        experiments = new Experiments();
    }

    @Step("Wait for startup")
    private void waitForStartup() {
        int errCount = 0;
        while (true) {
            try {
                if (tabBar.isDisplayed())
                    return;

                IntroScreen.getVisible();
                return;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                ++errCount;
                if (errCount >= 2)
                    throw e;

                if (new CrashDialog().isDisplayed())
                    throw new NoRetryException("Crash dialog detected");

                Dialog anrAlert = Dialog.newAnrAlert();
                if (anrAlert.isDisplayed())
                    anrAlert.tryClickAt("Подождать");
            }
        }
    }

    void doBeforeStart() {}

    void doSkipIntro() {
        skipIntro();
        tabBar.checkVisible();
    }

    void doEnd() {}

    @After
    public void tearDown() {
        if (user == null)
            return;

        try {
            doEnd();

            shouldNotSeeCrash();
        } finally {
            collectLogs();
            user.quit();
        }
    }

    private void collectLogs() {
        final LogManager logManager = user.getLogManager();
        Allure.addAttachment("logs", logManager.getAllLogs());
        Allure.addAttachment("experiments", logManager.getExperiments());
    }

    @Step("Should not see crash")
    private void shouldNotSeeCrash() {
        String error = detectCrash();
        if (error == null)
            return;

        final List<String> log = user.getLogManager().getCrashLog();
        if (!log.isEmpty())
            error += ":\n" + String.join("\n", log);

        if (error.startsWith("ANR"))  // try to repeat test in case of ANR
            throw new RuntimeException(error);
        throw new NoRetryException(error);
    }

    private String detectCrash() {
        if (user.getPlatform() == Platform.Android) {
            if (new CrashDialog().isDisplayed())
                return "Crash dialog detected";
            if (Dialog.newAnrAlert().isDisplayed()) {
                final byte[] traces = user.getDriver().pullFile("/data/anr/traces.txt");
                Allure.addAttachment("anr_traces.txt", new String(traces));
                return "ANR detected";
            }
        }

        final String status = user.checkAppIsRunning();
        if (status != null)
            return "Application crashed: " + status;

        return null;
    }

    // Dynamic step ---

    public static void step(String name, Runnable runnable) {
        String uuid = UUID.randomUUID().toString();
        StepResult result = new StepResult()
                .withName(name);
        Allure.getLifecycle().startStep(uuid, result);
        try {
            runnable.run();
            Allure.getLifecycle().updateStep(uuid, s -> s.withStatus(Status.PASSED));
        } catch (Throwable e) {
            Allure.getLifecycle().updateStep(uuid, s -> s
                    .withStatus(ResultsUtils.getStatus(e).orElse(Status.BROKEN))
                    .withStatusDetails(ResultsUtils.getStatusDetails(e).orElse(null)));
            throw e;
        } finally {
            Allure.getLifecycle().stopStep(uuid);
        }
    }

    public static void prepare(Runnable runnable) {
        step("Подготовка", runnable);
    }

    public static void prepare(String description, Runnable runnable) {
        step("Подготовка: " + description, runnable);
    }

    void prepareDisableMapsSearch() {
        prepareDisableMapsSearch(null);
    }

    /**
     * Makes sure NewSearch feature is disabled.
     *
     * Note: this method does not actually switch to OldSearch.
     */
    void prepareDisableMapsSearch(Runnable runnable) {
        prepare("Отключен поиск от МЯКа. "
            + "Developer Settings -> Search ->Y.Maps search for yandexoid -> off. "
            + "Developer Settings -> Search ->Y.Maps search screen -> off", () -> {
            experiments.disableMapsSearch().apply();

            if (runnable != null)
                runnable.run();
        });
    }

    public static void expect(String description, Runnable runnable) {
        step("Expectation: " + description, runnable);
    }

    // Actions -------------------------------------------------------------------------------------

    final void buildRoute() {
        buildRoute(ZELENOGRAD);
    }

    final void buildRoute(GeoPoint to) {
        buildRoute(null, to, null);
    }

    final void buildRoute(GeoPoint from, GeoPoint to) {
        buildRoute(from, to, null);
    }

    final void buildRouteAndGo(GeoPoint to) {
        buildRoute(null, to, null, true);
    }

    final void buildRouteAndGo(GeoPoint from, GeoPoint to) {
        buildRoute(from, to, null, true);
    }

    final void buildRouteAndGo(GeoPoint from, GeoPoint to, List<RoutePoint> via) {
        buildRoute(from, to, via, true);
    }

    final void buildRoute(GeoPoint from, GeoPoint to, List<RoutePoint> via, boolean accept) {
        OverviewScreen overviewScreen = buildRoute(from, to, via);
        if (accept)
            overviewScreen.clickGo();
    }

    OverviewScreen buildRoute(GeoPoint from, GeoPoint to, List<RoutePoint> via) {
        commands.buildRoute(from, to, via);
        return OverviewScreen.waitForRoute();
    }

    @Step("Построить маршрут и запустить симуляцию ведения")
    final void buildRouteAndStartSimulation() {
        buildRouteToSomePointAndGo();
        commands.toggleDebugDriving();
    }

    @Step("Построить маршрут в некоторую точку, нажать Поехали!")
    final void buildRouteToSomePointAndGo() {
        buildRouteAndGo(ZELENOGRAD);
    }

    @Step("Выйти из приложения по кнопке Back")
    final void closeAppByBack() {
        user.pressesBackButton();
    }

    @Step("Свернуть приложение по кнопке Home")
    final void closeAppByHome() {
        user.pressesHomeButton();
    }

    final void dismissPromoBanners() {
        PromoBanner.dismissIfVisible();
        MapPromoBanner.dismissIfVisible();
    }

    final void downloadCache(int regionId) {
        commands.downloadCache(regionId, Duration.ofMinutes(2));
    }

    @Step("Развернуть приложение")
    final void openApp() {
        user.activateApp();
        tabBar.checkVisible();
    }

    @Step("Сбросить маршрут")
    final void resetRoute() {
        mapScreen.clickResetRoute().clickAt("Да");
    }

    final void restartAppAndSkipIntro() {
        user.restartApp();
        skipIntro();
    }

    @Step("Rotate and return back")
    final void rotateAndReturn() {
        for (int i = 0; i < 2; ++i)
            user.rotates();
    }

    @Step("Изменить тему Навигатора -> {theme}")
    final NaviTheme setTheme(NaviTheme theme) {
        mapScreen.clickMenu().clickSettings().click("Карта и интерфейс",
            "Ночной режим", (theme == NaviTheme.DAY ? "Выключен" : "Включен"));
        tabBar.clickMap();
        return theme;
    }

    // Skips intro-screens and Searchlib splash screen.
    // Returns true if at least one intro-screen has been passed.
    final boolean skipIntro() {
        boolean skipped = new IntroScreen().skip();
        if (!mapScreen.isDisplayed())
            SearchlibScreen.dismissIfVisible();
        return skipped;
    }

    final void showPointYandex() {
        showPoint(YANDEX);
    }

    @Step("Show point {point.name}")
    final void showPoint(GeoPoint point) {
        commands.showPointOnMap(point, 16, true);
        user.waitFor(Duration.ofSeconds(3));
    }

    @Step("Запустить симуляцию движения по маршруту")
    final void toggleDebugDriving() {
        commands.toggleDebugDriving();
    }

    @Step("Запустить симуляцию движения по маршруту")
    final void toggleDebugDriving(double speed) {
        commands.toggleDebugDriving(speed);
    }

    @Step("Дождаться приезда в конечную точку")
    final void waitForFinish() {
        user.waitForLog("guidance.route_finish", Duration.ofMinutes(10));
    }

    // Tests ---------------------------------------------------------------------------------------
    void testBasicInterface() {
        tabBar.clickSearch();
        tabBar.clickBookmarks();
        tabBar.clickMenu();
        tabBar.clickMap();
    }

    // Experiments ---------------------------------------------------------------------------------
    static final class Experiment {
        static final String DAY_NIGHT_FAST_SWITCH = "navi_feature_day_night_fast_switch";
        static final String DISABLE_OVERVIEW_ADS = "navi_overview_ads_disabled";
        static final String GAS_STATIONS_FORCE_TEST_MODE
            = "navi_feature_gas_stations_force_test_mode";
        static final String GAS_STATIONS_LAYER = "navi_feature_gas_stations_layer";
        static final String IGNORE_ADS_COOLDOWN = "navi_feature_ignore_ads_cooldown";
        static final String MAPS_POI_SEARCH = "navi_feature_yandex_maps_poi";
        static final String MAPS_SEARCH = "navi_feature_yandex_maps_search_screen";
        static final String MASTERCARD = "navi_feature_mastercard";
        static final String NEW_OVERVIEW_SCREEN = "navi_feature_new_overview_screen";
        static final String NEW_OVERVIEW_SCREEN_TABS = "navi_feature_new_overview_screen_tabs";
        static final String OVERIVEW_ADS_PRIORITY = "navi_overview_ads_priority_for_RU";
        static final String PURE_PLATFORM_LAYOUT = "navi_feature_pure_platform_layout";
        static final String ROUTE_VARIANT_BALLOONS_OFF = "navi_feature_route_variant_balloons_off";
        static final String TURN_OFF_GAS_STATIONS_COVID_19_MAP_PROMO
            = "navi_feature_turn_off_gas_stations_covid_19_map_promo";
        static final String TAXI_AD_ROUTE_AT_SECOND_POSITION
            = "navi_feature_show_alternative_taxi_second";
        static final String TAXI_AD_ROUTE_AT_THIRD_POSITION
            = "navi_feature_show_alternative_taxi_third";
    }

    public class Experiments {
        private final HashMap<String, Object> params = new HashMap<>();

        Experiments enable(String... experiments) {
            return doSet(true, experiments);
        }

        Experiments disable(String... experiments) {
            return doSet(false, experiments);
        }

        // TODO: MOBNAVI-19158: Fix tests with MYaK search
        Experiments disableMapsSearch() {
            return doSet(false, "navi_feature_yandex_maps_search_screen");
        }

        Experiments set(String experiment, boolean value) {
            return doSet(value, experiment);
        }

        Experiments set(String experiment, String value) {
            return doSet(value, experiment);
        }

        private Experiments doSet(boolean value, String... experiments) {
            return doSet(value ? "enabled" : "disabled", experiments);
        }

        private Experiments doSet(String value, String... experiments) {
            for (String experiment : experiments)
                params.put(experiment, value);
            return this;
        }

        void applyAndRestart() {
            apply();
            restartAppAndSkipIntro();
        }

        void apply() {
            applyOnly();
            IntroScreen.getVisible().clickAction();
        }

        void applyOnly() {
            commands.addExp(params);
            params.clear();
        }
    }

    @Step("Включить Dev. Settings - Ad - Force datatesting environment")
    final void enableForceDatatesting() {
        tabBar.clickMenu().clickSettings().click("Developer settings",
            "Ad", "Force datatesting environment");
        tabBar.clickMap();
    }

}
