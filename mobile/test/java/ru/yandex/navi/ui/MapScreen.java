package ru.yandex.navi.ui;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.MobileElement;
import io.appium.java_client.MultiTouchAction;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.navi.NaviTheme;
import ru.yandex.navi.RouteColor;
import ru.yandex.navi.tf.Direction;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.Screenshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static io.appium.java_client.touch.offset.PointOption.point;
import static java.lang.Integer.max;

public final class MapScreen extends BaseScreen {
    @AndroidFindBy(id = "bugbutton")
    @iOSXCUITFindBy(accessibility = "bugbutton")
    private MobileElement bugBtn;

    @AndroidFindBy(id = "addroadeventbutton")
    @iOSXCUITFindBy(accessibility = "addroadeventbutton")
    private MobileElement addRoadEventBtn;

    @AndroidFindBy(id = "cancelbutton")
    @iOSXCUITFindBy(accessibility = "cancelbutton")
    private MobileElement cancelBtn;

    @AndroidFindBy(id = "voicebutton")
    @iOSXCUITFindBy(accessibility = "voicebutton")
    private MobileElement voiceBtn;

    @AndroidFindBy(id = "button_parkingtraffic")
    @iOSXCUITFindBy(accessibility = "button_parkingtraffic")
    private MobileElement parkingTrafficBtn;

    @AndroidFindBy(id = "parkingroutebutton")
    @iOSXCUITFindBy(accessibility = "parkingroutebutton")
    public MobileElement parkingRouteBtn;

    @AndroidFindBy(id = "compassbutton")
    @iOSXCUITFindBy(accessibility = "compassbutton")
    private MobileElement compassBtn;

    @AndroidFindBy(id = "findmebutton")
    @iOSXCUITFindBy(accessibility = "findmebutton")
    MobileElement findmeBtn;

    @AndroidFindBy(id = "minusbutton")
    @iOSXCUITFindBy(accessibility = "minusbutton")
    public MobileElement minusBtn;

    @AndroidFindBy(id = "plusbutton")
    @iOSXCUITFindBy(accessibility = "plusbutton")
    public MobileElement plusBtn;

    @AndroidFindBy(id = "group_progresseta")
    @iOSXCUITFindBy(accessibility = "ContextETAView")
    private MobileElement panelEta;

    @AndroidFindBy(id = "resetroutebutton")
    @iOSXCUITFindBy(id = "ETAResetButton")
    private MobileElement resetRouteBtn;

    @AndroidFindBy(id = "textview_suggestion")
    @iOSXCUITFindBy(accessibility = "suggestions_item_view")
    public MobileElement suggestItem;

    private final ParkingTrafficButton parkingTrafficButton;

    public final TabBar tabBar;
    public final MobileElement[] mainButtons;
    public final OfflineResultsPanel offlineResultsPanel;

    private static final int COLOR_ROUTE_FREE = 0x82ea0e;
    private static final int COLOR_ROUTE_LIGHT = 0xffff41;
    private static final int COLOR_ROUTE_HARD = 0xff5413;

    private static final Set<Integer> CURSOR_COLORS = toColorSet(
            /* v1 */ 0xdfab16, 0xf8cc4b, 0xf8cc4c,
            /* v2 */ 0xe09b2b, 0xeca42d, 0xf6b900, 0xfce859);

    private static final Set<Integer> GAS_PINS_COLORS
        = toColorSet(/* day */ 0x00e300, 0x00e500, /* night */ 0x00ff00);

    private static final Set<Integer> PARKING_COLORS
        = toColorSet(/* day */ 0x2eb5e2, /* night s*/ 0x29afc9);

    // from route_line_impl.cpp
    private static final Map<RouteColor, Set<Integer>> routeColors = ImmutableMap.of(
            RouteColor.OFFLINE, toColorSet(0x177ee6),
            RouteColor.ONLINE, toColorSet(COLOR_ROUTE_FREE, COLOR_ROUTE_LIGHT, COLOR_ROUTE_HARD));

    private static final Set<Integer> mapColors = toColorSet(
        /* day */ 0x77dced, 0xd6ecdb, 0xf2f1ed, 0xfffdf6, 0xfffdf7, 0xe09f00, 0x639b33, 0xebebeb,
        /* night */ 0x1d2430, 0x263e37, 0x718cb3, 0x040e1c, 0x2d3850, 0x718bb3, 0x425982
    );

    private static final Random random = new Random();

    private static Set<Integer> toColorSet(Integer... colors) {
        return new HashSet<>(Arrays.asList(colors));
    }

    public MapScreen() {
        super();
        parkingTrafficButton = new ParkingTrafficButton();
        tabBar = new TabBar();
        mainButtons = new MobileElement[] {
                bugBtn, addRoadEventBtn, voiceBtn, parkingTrafficBtn, plusBtn, minusBtn,
                compassBtn, findmeBtn
        };
        offlineResultsPanel = new OfflineResultsPanel(this);
    }

    public static MapScreen getVisible() {
        MapScreen screen = new MapScreen();
        screen.checkVisible();
        return screen;
    }

    @Override
    public boolean isDisplayed() {
        int errCount = 0;
        while (true) {
            try {
                return MobileUser.isDisplayed(plusBtn) || MobileUser.isDisplayed(panelEta);
            } catch (StaleElementReferenceException e) {
                ++errCount;
                System.err.println(String.format("MapScreen.isDisplayed() failed: %s", e));
                if (errCount >= 2)
                    throw e;
            }
        }
    }

    public void buildRouteByLongTapAndGo() {
        longTap().clickTo();
        OverviewScreen.waitForRoute().clickGo();
    }

    public void buildRouteBySearchAndGo(String destination) {
        clickSearch().searchFor(destination).clickFirstItem(destination).expectGeoCard().clickGo();
        OverviewScreen.waitForRoute().clickGo();
    }

    @Step("Добавить точку в блок 'Избранное'")
    public void addBookmarkByLongTap(String name) {
        longTap().clickMyPlaces().saveToFavorites(name);
    }

    public RoadEventPanel addRoadEvent() {
        tryDo(() -> addRoadEventBtn.click());
        return RoadEventPanel.getVisible();
    }

    public void checkCursor(boolean expectedVisible) {
        double fractionCursorColors = user.getImageForScreen().getFractionOfColors(CURSOR_COLORS);
        boolean isVisible = fractionCursorColors > .001;
        Assert.assertEquals("Cursor isVisible=" + fractionCursorColors,
            expectedVisible, isVisible);
    }

    public void checkGasStationsLayer(boolean expectedVisible) {
        double fraction = user.getImageForScreen().getFractionOfColors(GAS_PINS_COLORS);
        System.err.println(String.format("checkGasStationsLayer: expected=%b, fraction=%f",
            expectedVisible, fraction));
        boolean isVisible = fraction > .005;
        Assert.assertEquals("Gas Stations Layer isVisible=" + fraction,
            expectedVisible, isVisible);
    }

    public void checkPanelEta() {
        checkPanelEta(true);
    }

    public void checkPanelEta(boolean visible) {
        if (visible)
            user.shouldSee(panelEta);
        else
            user.shouldNotSee(panelEta);
    }

    public void checkPanelEta(Duration timeout) {
        user.shouldSee(panelEta, timeout);
    }

    public void checkParkingLayer(boolean expectedVisible) {
        double fraction = user.getImageForScreen().getFractionOfColors(PARKING_COLORS);
        System.err.println(String.format("checkParkingLayer: expected=%b, fraction=%f",
            expectedVisible, fraction));
        boolean isVisible = fraction > .00003;
        Assert.assertEquals("Parking isVisible=" + fraction, expectedVisible, isVisible);
    }

    public void checkRouteColor(RouteColor color) {
        double fraction = user.getImageForScreen().getFractionOfColors(routeColors.get(color));
        System.err.println(String.format("checkRouteColor(%s): fraction=%f", color, fraction));
        Assert.assertTrue("Route color is " + color, fraction > .002);
    }

    @Step("Нажать кнопку '{button.name}' на таббаре")
    public void click(TabBar.Button button) {
        if (button == TabBar.OVERVIEW)
            user.shouldSee(panelEta);

        doClick(button);
    }

    @Step("Нажать на кнопку 'Мои Места' на таббаре")
    public BookmarksScreen clickBookmarks() {
        doClick(TabBar.BOOKMARKS);
        return BookmarksScreen.getVisible();
    }

    @Step("Нажать на кнопку 'Обзор' на таббаре")
    public OverviewScreen clickOverview() {
        doClick(TabBar.OVERVIEW);
        return OverviewScreen.getVisible();
    }

    @Step("Нажать кнопку отмены поиска")
    public void cancelSearch() {
        user.clicks(cancelBtn);
    }

    @Step("Нажать на значок компаса внизу справа")
    public void clickCompass() {
        user.clicks(compassBtn);
    }

    @Step("Нажать на кнопку определения местоположения")
    public void clickFindMe() {
        user.clicks(findmeBtn);
    }

    @Step("Нажать на кнопку 'Меню' на таббаре")
    public MenuScreen clickMenu() {
        doClick(TabBar.MENU);
        return MenuScreen.getVisible();
    }

    @Step("Нажать на кнопку 'Поиск' на таббаре")
    public SearchScreen clickSearch() {
        doClick(TabBar.SEARCH);
        return SearchScreen.getVisible();
    }

    @Step("Нажать на кнопку 'Яндекс.Музыка' на таббаре")
    public MusicScreen clickMusic() {
        doClick(TabBar.MUSIC);
        return MusicScreen.getVisible();
    }

    private void doClick(TabBar.Button button) {
        tryDo(() -> tabBar.getButton(button).click());
    }

    private void tryDo(Runnable runnable) {
        final Point center = user.getWindowCenter();
        int errCount = 0;
        while (true) {
            try {
                runnable.run();
                break;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                ++errCount;
                System.err.println(String.format("tryDo #%d: %s", errCount, e));
                if (errCount >= 5)
                    throw e;
                doTap(center);
            }
        }
    }

    @Step("Тап на кнопку вызова голосового помощника")
    public void clickVoice() {
        user.clicks(voiceBtn);
    }

    @Step("Тапнуть на саджест точки назначения внизу экрана")
    public void clickSuggestItem() {
        user.clicks(suggestItem);
    }

    public void checkFreeDrive() {
        user.shouldNotSee(tabBar);
        user.shouldNotSee(mainButtons);
    }

    public void cancelCovidSearch() {
        if (isSearchActive())
            cancelSearch();

        final SearchResultsCard searchResultsCard = SearchResultsCard.get();
        if (searchResultsCard.isDisplayed())
            searchResultsCard.clickClose();
    }

    private boolean isSearchActive() {
        return MobileUser.isDisplayed(cancelBtn);
    }

    public void checkSearchIsActive(boolean isActive) {
        if (isActive)
            user.shouldSee(cancelBtn);
        else
            user.shouldSee(voiceBtn);
    }

    public void expectTheme(NaviTheme theme) {
        Assert.assertEquals(theme, getTheme());

        for (MobileElement element : new MobileElement[]{
                plusBtn, panelEta, suggestItem, tabBar.getElement()}) {
            int errCount = 0;
            while (true) {
                try {
                    if (MobileUser.isDisplayed(element))
                        Assert.assertEquals(theme, getThemeFor(element));
                    break;
                } catch (StaleElementReferenceException e) {
                    ++errCount;
                    System.err.println(
                        String.format("expectTheme failed for element %s: %s", element, e));
                    if (errCount >= 2)
                        throw e;
                }
            }
        }
    }

    @Step("Нажать на кнопку сброса маршрута (Х), находящуюся на плашке ETA")
    public Dialog clickResetRoute() {
        user.clicks(resetRouteBtn);
        return Dialog.withTitle("Вы действительно хотите сбросить маршрут?");
    }

    public NaviTheme getTheme() {
        return getThemeFor(user.getGrayColorForScreen());
    }

    private NaviTheme getThemeFor(MobileElement element) {
        return getThemeFor(user.getGrayColorFor(element));
    }

    private static NaviTheme getThemeFor(int color) {
        return color > 128 ? NaviTheme.DAY : NaviTheme.NIGHT;
    }

    public void zoomIn(int count) {
        for (int i = 0; i < count; ++i)
            zoomIn();
    }

    public void zoomIn() {
        user.clicks(plusBtn);
    }

    public void zoomOut(int count) {
        for (int i = 0; i < count; ++i)
            zoomOut();
    }

    public void zoomOut() {
        user.clicks(minusBtn);
    }

    public void checkMainButtons() {
        user.shouldSee(mainButtons);
    }

    public void checkTabBarAndMainButtonsAppearAfterTap() {
        ArrayList<MobileElement> elements = new ArrayList<>();
        elements.add(tabBar.getElement());
        elements.addAll(Arrays.asList(mainButtons));

        int errCount = 0;
        final int maxErrors = max(5, elements.size());
        for (MobileElement element : elements) {
            while (true) {
                try {
                    user.shouldSee(element);
                    break;
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    ++errCount;
                    System.err.println(String.format(
                        "checkTabBarAndMainButtonsAppearAfterTap #%d: %s", errCount, e));
                    if (errCount >= maxErrors)
                        throw e;
                    tap2();
                }
            }
        }
    }

    @Step("Выполнить лонгтап в центре карты")
    public LongTapMenu longTap() {
        return doLongTap(0.5, 0.5);
    }

    @Step("Выполнить лонгтап по карте ({x}, {y})")
    public LongTapMenu longTap(double x, double y) {
        return doLongTap(x, y);
    }

    private LongTapMenu doLongTap(double x, double y) {
        user.longTap(String.format("(%.1f, %.1f)", x, y), user.getRelativePoint(x, y));
        return LongTapMenu.getVisible();
    }

    @Step("Жест: pinch")
    public void pinch() {
        user.readLogs();
        user.pinch(false, user.getRelativePoint(0.8, 0.2));
        user.waitForLog("zoom: out");
    }

    @Step("Поворачивать карту двумя пальцами движением по кругу")
    public void rotate() {
        user.rotate();
    }

    @Step("Жест: spread")
    public void spread() {
        user.readLogs();
        user.pinch(true, user.getRelativePoint(0.8, 0.2));
        user.waitForLog("zoom: in");
    }

    @Step("Сдвинуть карту {direction}")
    public MapScreen swipe(Direction direction) {
        Dimension size = user.getWindowSize();
        int y0 = size.height / 2, y1 = y0;
        int x0 = size.width / 2, x1 = x0;

        switch (direction) {
            case LEFT: x0 = size.width * 8 / 10; x1 = size.width * 2 / 10; break;
            case RIGHT: x0 = size.width * 2 / 10; x1 = size.width * 8 / 10; break;
            case UP:
                y0 = 5 * size.height / 10;  // to not swipe geo-card
                y1 = size.height / 10;
                break;
            case DOWN: y0 = size.height / 10; y1 = 9 * size.height / 10; break;
            default: throw new AssertionError("Unexpected direction: " + direction);
        }

        user.swipe(x0, y0, x1, y1);

        return this;
    }

    @Step("Тапнуть на карту")
    public void tap() {
        user.tap("карта", user.getWindowCenter());
    }

    @Step("Тапнуть* на карту")
    public void tapMap() {
        user.tap("карта", findMapPoint());
    }

    private Point findMapPoint() {
        final Point center = user.getWindowCenter();
        final Screenshot screenshot = user.getImageForScreen();

        for (int i = 0; i < 50; ++i) {
            final Point point = nextPoint(center);
            if (screenshot.isMapPoint(point, mapColors))
                return point;
        }

        return center;
    }

    private Point nextPoint(Point center) {
        final int size = Math.min(center.x, center.y) - 10;
        return new Point(center.x + nextInt(size), center.y + nextInt(size));
    }

    private int nextInt(int size) {
        return random.nextInt(2 * size) - size;
    }

    @Step("Тапнуть** на карту")
    public void tap2() {
        doTap(user.getWindowCenter());
    }

    @Step("Нажатием на свободное место на карте закрыть голосового помощника")
    public void tapToCloseAlice() {
        doTap(user.getRelativePoint(0.5, 0.3));
    }

    private void doTap(Point point) {
        new MultiTouchAction(user.getDriver())
                .add(user.newTouchAction().tap(point(point.x - 10, point.y)))
                .add(user.newTouchAction().tap(point(point.x + 10, point.y)))
                .perform();
    }

    @Step("Выполнить лонгтап по кнопке парковочного слоя")
    public void longTapParkingButton() {
        int errCount = 0;
        while (true) {
            try {
                user.longTap("button.parking", parkingTrafficButton.parking());
                break;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                ++errCount;
                if (errCount >= 5)
                    throw e;
                tap2();
            }
        }

        user.shouldSee(panelEta);
    }

    @Step("Выполнить тап по кнопке парковочного слоя")
    public void tapParkingButton() {
        user.tap("button.parking", parkingTrafficButton.parking());
        user.shouldSee(parkingRouteBtn);
    }

    @Step("Тапнуть по кнопке показа пробок")
    public void tapTrafficButton() {
        user.tap("button.traffic", parkingTrafficButton.traffic());
    }

    @Step("Тапнуть по кнопке построения парковочного маршрута")
    public MapScreen clickParkingRouteButton() {
        user.clicks(parkingRouteBtn);
        return this;
    }

    public GeoCard searchAndClickFirstItem(String text) {
        return clickSearch().searchAndClickFirstItem(text);
    }
}
