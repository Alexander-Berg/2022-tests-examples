package ru.yandex.navi.tf;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.MultiTouchAction;
import io.appium.java_client.NoSuchContextException;
import io.appium.java_client.TouchAction;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import io.qameta.allure.Step;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import static io.appium.java_client.touch.WaitOptions.waitOptions;
import static io.appium.java_client.touch.offset.PointOption.point;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public abstract class MobileUser {
    private static final int MAX_SWIPES = 25;
    private static final int SWIPE_DELTA = 400;
    private static MobileUser user;

    private ScreenOrientation screenOrientation = null;
    private LogManager logManager;
    private SoundDecoder soundDecoder;
    private String soundPattern;
    protected UserCapabilities userCaps;
    private MobileUserListener listener;
    private boolean expectRunning = true;
    private String resPath;

    enum ByText {
        Match,
        Regex,
    }

    protected MobileUser() {}

    public static MobileUser create(UserCapabilities userCaps, MobileUserListener listener) {
        if (userCaps.platform == null)
            userCaps.platform = detectPlatform();

        if (userCaps.platform == Platform.iOS) {
            if (userCaps.platformVersion == null)
                userCaps.platformVersion = getEnv("PLATFORM_VERSION", "12.2");
            user = new IosUser();
        }
        else {
            user = new AndroidUser();
        }

        user.userCaps = userCaps;
        user.listener = listener;
        user.createDriver(true);

        return user;
    }

    public static MobileUser getUser() {
        assert user != null;
        return user;
    }

    public final void setSoundDecoder(String soundPattern, SoundDecoder soundDecoder) {
        this.soundPattern = soundPattern;
        this.soundDecoder = soundDecoder;
    }

    private static Platform detectPlatform() {
        String platformName = getOptEnv("PLATFORM");
        if ("ios".equalsIgnoreCase(platformName))
            return Platform.iOS;
        if ("android".equalsIgnoreCase(platformName))
            return Platform.Android;

        final String appPath = getOptEnv("APP_PATH");
        if (appPath != null) {
            if (appPath.endsWith(".app"))
                return Platform.iOS;
            if (appPath.endsWith(".apk"))
                return Platform.Android;
        }

        platformName = System.getProperty("os.name");
        if (platformName.contains("Mac"))
            return Platform.iOS;

        return Platform.Android;
    }

    static DesiredCapabilities createDefaultCaps(UserCapabilities userCaps) {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("language", "ru");
        caps.setCapability("locale", userCaps.platform == Platform.iOS ? "ru_RU" : "ru");

        caps.setCapability("orientation",
            Optional.ofNullable(userCaps.screenOrientation).orElse(ScreenOrientation.PORTRAIT));

        return caps;
    }

    public final Platform getPlatform() {
        return userCaps.platform;
    }

    final String getPlatformVersion() {
        return userCaps.platformVersion;
    }

    final void setPlatformVersion(String value) {
        userCaps.platformVersion = value;
    }

    public final int getMajorPlatformVersion() {
        assert userCaps.platformVersion != null;
        return Integer.parseInt(userCaps.platformVersion.split("\\.")[0]);
    }

    @Step("Выгрузить приложение")
    public final void stopApp() {
        getDriver().closeApp();
        expectRunning = false;
    }

    public final MobileElement findElementByImage(String imageRes) {
        return getDriver().findElementByImage(getImageB64(imageRes));
    }

    public final List<MobileElement> findElementsByImage(String imageRes) {
        return getDriver().findElementsByImage(getImageB64(imageRes));
    }

    private String getImageB64(String imageRes) {
        try {
            final String resName = getResPath() + "/" + imageRes + ".png";
            final URL refImgUrl = getClass().getClassLoader().getResource(resName);
            if (refImgUrl == null)
                throw new NoRetryException("Can't load image: " + resName);
            final File refImgFile = Paths.get(refImgUrl.toURI()).toFile();
            return Base64.getEncoder().encodeToString(Files.readAllBytes(refImgFile.toPath()));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Can't read resource '" + imageRes + "'", e);
        }
    }

    private String getResPath() {
        if (resPath == null) {
            final Dimension size = getWindowSize();
            final int maxSize = Integer.max(size.height, size.width);
            resPath = "images/" + (maxSize > 1500 ? "960" : "640");
        }
        return resPath;
    }

    @Step("Activate App")
    public final void activateApp() {
        getDriver().activateApp(getBundleId());
        expectRunning = true;
        waitFor(Duration.ofSeconds(3));
    }

    void createDriver(boolean reset) {
        doCreateDriver(reset);
        listener.onDriverCreated(this);
    }

    protected abstract void doCreateDriver(boolean reset);

    protected abstract String getBundleId();

    protected abstract String getClipboard();

    @Step("Перезапустить приложение")
    public final void restartApp() {
        doRestartApp();
    }

    public abstract void doRestartApp();

    public abstract void restartActivity();

    @Step("Run app in background")
    public final void runAppInBackground() {
        getDriver().runAppInBackground(Duration.ofSeconds(-1));
    }

    public int getGrayColorForScreen() {
        return getImageForScreen().getGrayColor();
    }

    public int getGrayColorFor(MobileElement element) {
        return getImageFor(element).getGrayColor();
    }

    public Screenshot getImageForScreen() {
        return Screenshot.fromBytes(getDriver().getScreenshotAs(OutputType.BYTES));
    }

    private Screenshot getImageFor(MobileElement element) {
        return Screenshot.fromBytes(element.getScreenshotAs(OutputType.BYTES));
    }

    public final void checkClipboard(Pattern pattern) {
        final String data = getClipboard();
        Assert.assertNotNull("Empty clipboard", data);
        Assert.assertTrue(
            String.format("Clipboard data '%s' doesn't match pattern '%s'", data,
                pattern.pattern()),
            pattern.matcher(data).find());
    }

    public final void hideKeyboard() {
        getDriver().hideKeyboard();
    }

    public final MobileElement findElementByText(String value) {
        return findElementByText(value, Direction.NONE);
    }

    private MobileElement findElementByText(String value, Direction direction) {
        return findElementBy(ByText.Match, value, direction);
    }

    public final MobileElement findElementByRegex(String value) {
        return findElementBy(ByText.Regex, value, Direction.NONE);
    }

    private MobileElement findElementBy(ByText byText, String value, Direction direction) {
        final int swipeDelta = (direction == Direction.UP ? -SWIPE_DELTA : SWIPE_DELTA);
        for (int i = 0; i < MAX_SWIPES; ++i) {
            MobileElement element = findElementsBy(byText, value).stream().findFirst().orElse(null);
            if (element != null && element.isDisplayed()) {
                if (direction == Direction.DOWN)
                    checkOverlappingTabbar(element);
                return element;
            }
            if (direction == Direction.NONE)
                break;
            swipe(swipeDelta);
        }
        throw new NoSuchElementException(String.format("Cannot find element '%s'", value));
    }

    public final MobileElement findElementByTextIgnoreCase(String value) {
        for (int i = 0; i < 2; ++i) {
            final String text = (i == 0 ? value : value.toUpperCase());
            MobileElement element = findElementsByText(text).stream().findFirst().orElse(null);
            if (element != null && element.isDisplayed())
                return element;
        }
        throw new NoSuchElementException(String.format("Cannot find element '%s'", value));
    }

    public abstract List<MobileElement> findElementsBy(ByText byText, String value);

    private List<MobileElement> findElementsByRegex(String value) {
        return findElementsBy(ByText.Regex, value);
    }

    public final List<MobileElement> findElementsByText(String value) {
        return findElementsBy(ByText.Match, value);
    }

    public final List<MobileElement> findElementsByTextFrom(MobileElement parent, String value) {
        return parent.findElementsByXPath(String.format("//*[@text='%s']", value));
    }

    @Step("Clicks on {element}")
    public void clicks(MobileElement element) {
        shouldSee(element);
        element.click();
    }

    public final void clicksInScrollable(MobileElement element) {
        shouldSeeInScrollable(element);
        clicks(element);
    }

    public final void clickInHorizontalList(MobileElement element, MobileElement horizontalList) {
        shouldSeeInHorizontalList(element, horizontalList);
        clicks(element);
    }

    @Step("Clicks on '{text}'")
    public final void clicks(String text) {
        clicks(findElementByText(text));
    }

    @Step("Clicks on '{text}'")
    public final void clicks(String text, Direction direction) {
        clicks(findElementByText(text, direction));
    }

    public abstract void pressesBackButton();
    public abstract void navigateBack();

    public abstract void makePhoneCall();

    @Step("Types {text} into {element}")
    public final void types(MobileElement element, String text) {
        element.sendKeys(text);
    }

    @Step("Swipes up {element}")
    public final void swipesUp(MobileElement element) {
        org.openqa.selenium.Point from = element.getCenter();
        swipe(from.x, from.y, from.x, from.y - 300);
    }

    private ApplicationState queryAppState() {
        return getDriver().queryAppState(getBundleId());
    }

    public final String checkAppIsRunning() {
        if (!expectRunning)
            return null;

        waitFor(Duration.ofSeconds(1));

        ApplicationState appState = queryAppState();
        if (appState != ApplicationState.RUNNING_IN_FOREGROUND
            && appState != ApplicationState.RUNNING_IN_BACKGROUND) {
            return "appState=" + appState;
        }

        return null;
    }

    public final void shouldSee(MobileElement element) {
        if (!element.isDisplayed()) {
            throw new NoSuchElementException(
                String.format("Element '%s' is not displayed.", element));
        }
    }

    public final void shouldNotSee(MobileElement element) {
        assertFalse(String.format("Element '%s' is displayed.", element), isDisplayed(element));
    }

    public final void shouldNotSee(Displayable element, Duration duration) {
        shouldSee(() -> !element.isDisplayed(), duration,
            String.format("Element '%s' is displayed.", element));
    }

    public final void shouldSee(String item) {
        shouldSee(item, Direction.NONE);
    }

    public final void shouldSee(String item, Duration time) {
        shouldSee(() -> isDisplayed(findElementByRegex(item)), time);
    }

    public final void shouldSeeAnyOf(final String... items) {
        for (String item : items) {
            try {
                findElementBy(ByText.Regex, item, Direction.NONE);
                return;
            } catch (NoSuchElementException ignore) {
            }
        }
        Assert.fail("Elements not displayed: " + String.join(", ", items));
    }

    public final void shouldSeeAll(final String... items) {
        shouldSeeAll(Direction.NONE, items);
    }

    public final void shouldSeeAll(Direction direction, final String... items) {
        for (String item : items)
            shouldSee(item, direction);
    }

    public final void shouldSee(String item, Direction direction) {
        try {
            findElementBy(ByText.Regex, item, direction);
        }
        catch (NoSuchElementException e) {
            Assert.fail(String.format("Element '%s' is not displayed", item));
        }
    }

    public final void shouldNotSee(String item) {
        for (MobileElement element : findElementsByRegex(item)) {
            if (isDisplayed(element)) {
                Assert.fail(String.format("Element '%s' is displayed", item));
                break;
            }
        }
    }

    public final void shouldSee(final MobileElement[] elements) {
        for (MobileElement element : elements)
            shouldSee(element);
    }

    public final void shouldNotSee(final MobileElement[] elements) {
        for (MobileElement element : elements)
            shouldNotSee(element);
    }

    @Step("Should see in scrollable")
    public final void shouldSeeInScrollable(MobileElement element) {
        assertTrue(String.format("Element '%s' is not displayed.", element),
                isDisplayedInScrollable(element));
    }

    @Step("Should see in horizontally scrollable list")
    public final void shouldSeeInHorizontalList(MobileElement element,
                                                MobileElement horizontalList) {
        assertTrue(String.format("Element '%s' is not displayed in horizontal list.", element),
            isDisplayedInHorizontalList(element, horizontalList));
    }

    public void shouldSee(Displayable page) {
        assertTrue(String.format("Screen '%s' is not displayed.", page), page.isDisplayed());
    }

    public void shouldSee(Displayable page, Duration timeout) {
        shouldSee(page::isDisplayed, timeout, String.format("Screen '%s'", page));
    }

    public void shouldSee(MobileElement element, Duration timeout) {
        shouldSee(() -> isDisplayed(element), timeout, String.format("Element '%s", element));
    }

    public final void shouldSeeSuggest(List<MobileElement> items) {
        shouldSee("Suggest", items, Duration.ofSeconds(10));
    }

    public final void shouldSee(String name, List<MobileElement> items, Duration timeout) {
        shouldSee(() -> !items.isEmpty(), timeout, name);
    }

    public void shouldSee(BooleanSupplier supplier, Duration waitTime, String name) {
        final long endTime = System.currentTimeMillis() + waitTime.toMillis();

        while (true) {
            boolean stop = System.currentTimeMillis() > endTime;

            try {
                if (supplier.getAsBoolean())
                    return;
            }
            catch (NoSuchElementException e) {
                System.err.print(String.format("%s is not displayed", name));
            }

            if (stop)
                throw new NoSuchElementException(String.format("%s is not displayed", name));

            sleep(Duration.ofMillis(500));
        }
    }

    public void shouldNotSee(Displayable page) {
        assertFalse(String.format("Screen '%s' is displayed.", page), page.isDisplayed());
    }

    public final void shouldSeeInWebView(String text) {
        shouldSeeInWebView(text, Duration.ofSeconds(5));
    }

    public final void shouldSeeInWebView(String text, Duration timeout) {
        try {
            switchToWebView();
            shouldSee(() -> getDriver().getPageSource().contains(text), timeout, text);
        }
        catch (NoSuchContextException e) {
            System.err.println("MobileUser.shouldSeeInWebView failed: " + e.toString());
        }
        finally {
            switchToNative();
        }
    }

    private void switchToWebView() {
        for (String context : getDriver().getContextHandles()) {
            if (context.startsWith("WEBVIEW_ru.yandex.yandexnavi"))
                getDriver().context(context);
        }
    }

    private void switchToNative() {
        getDriver().context("NATIVE_APP");
    }

    public void quit() {
        getDriver().quit();
    }

    public static boolean isDisplayed(MobileElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public abstract void pressesHomeButton();

    @Step("Изменить ориентацию девайса на противоположную")
    public final void rotates() {
        setScreenOrientation(oppositeOf(getOrientation()));
    }

    private static ScreenOrientation oppositeOf(ScreenOrientation orientation) {
        return orientation == ScreenOrientation.LANDSCAPE ? ScreenOrientation.PORTRAIT :
                ScreenOrientation.LANDSCAPE;
    }

    @Step("Изменить ориентацию девайса на {orientation}")
    public final void rotatesTo(ScreenOrientation orientation) {
        setScreenOrientation(orientation);
    }

    public ScreenOrientation getOrientation() {
        if (screenOrientation == null)
            screenOrientation = getDriver().getOrientation();
        return screenOrientation;
    }

    public abstract void shouldNotSeeBackgroundGuidance();

    @Step("Taps on '{name}' {point}")
    public final void tap(String name, Point point) {
        assert name != null;
        newTouchAction().tap(point(point.x, point.y)).perform();
    }

    public void longTap(String name, Point point) {
        longTap(name, point, Duration.ofSeconds(1));
        assert name != null;
    }

    @Step("Long tap on '{name}' {point}")
    public void longTap(String name, Point point, Duration duration) {
        assert name != null;
        newTouchAction()
            .press(point(point.x, point.y))
            .waitAction(waitOptions(duration))
            .release()
            .perform();
    }

    @Step("Open URL {query} with params {params}")
    public final void openNaviUrl(String query, Map<String, Object> params) {
        try {
            URIBuilder uri = new URIBuilder("yandexnavi://" + query);
            if (params != null)
                params.forEach((key, value) -> uri.addParameter(key, value.toString()));
            doOpenUrl(uri.toString());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doOpenUrl(String url) {
        getDriver().get(url);
    }

    public abstract AppiumDriver<MobileElement> getDriver();

    public abstract void setAirplaneMode(boolean value);

    public abstract void toggleLocationServices();

    @Step("Set location {location}")
    public void setLocation(Location location) {
        getDriver().setLocation(location);
    }

    public LogManager getLogManager() {
        if (logManager == null) {
            logManager = new LogManager(this);
        }
        return logManager;
    }

    public void swipe(int fromX, int fromY, int toX, int toY) {
        newTouchAction()
                .press(point(fromX, fromY))
                .waitAction(waitOptions(Duration.ofMillis(1000)))
                .moveTo(point(toX, toY))
                .release()
                .perform();
    }

    @Step("Wait for {time.seconds} seconds")
    public void waitFor(Duration time) {
        long endTime = System.currentTimeMillis() + time.toMillis();
        new WebDriverWait(getDriver(), time.getSeconds() + 1).until(
                driver -> System.currentTimeMillis() > endTime);
    }

    public void waitFor(Displayable element, Duration time) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), time.getSeconds() + 1);
            wait.until(driver -> element.isDisplayed());
        } catch (RuntimeException e) {
            throw new NoSuchElementException("Element not found: " + element, e);
        }
    }

    public void waitForManeuverAnnotations() {
        waitForAnnotations(Duration.ofMinutes(1), "поверните");
    }

    public void waitForAnnotations(Duration time, String... expected) {
        waitForAnnotationsEx(time, expected, null);
    }

    public void waitForAnnotationsEx(Duration time, String[] expected, String[] unexpected) {
        final long endTime = System.currentTimeMillis() + time.toMillis();
        ArrayList<String> expectedAnnotations = new ArrayList<>(Arrays.asList(expected));

        while (true) {
            boolean stop = (System.currentTimeMillis() > endTime);
            while (true) {
                String line = getLogManager().find(soundPattern);
                if (line == null)
                    break;
                line = line.substring(line.indexOf(':') + 1);
                final String annotation = soundDecoder.decode(line);
                System.err.println("Annotation: " + annotation);

                expectedAnnotations.removeIf(annotation::contains);
                if (expectedAnnotations.isEmpty() && unexpected == null)
                    return;

                if (unexpected != null) {
                    if (Arrays.stream(unexpected).anyMatch(annotation::contains)) {
                        Assert.fail("Unexpected annotation: " + annotation);
                        return;
                    }
                }
            }
            if (stop)
                break;

            sleep(Duration.ofMillis(500));
        }

        Assert.assertTrue("Missed annotations: " + expectedAnnotations,
            expectedAnnotations.isEmpty());
    }

    private void sleep(Duration time) {
        try {
            Thread.sleep(time.toMillis());
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Unexpected interrupt", e);
        }
    }

    private boolean isDisplayedInScrollable(MobileElement element) {
        return swipeTo(element);
    }

    private boolean isDisplayedInHorizontalList(MobileElement element,
                                                MobileElement horizontalList) {
        return swipeToElementInHorizontalList(element, horizontalList);
    }

    @Step("Swipe to {element}")
    private boolean swipeTo(MobileElement element) {
        return swipeTo(element, Direction.UP) || swipeTo(element, Direction.DOWN);
    }

    @Step("Swipe to {element} in {horizontalList}")
    private boolean swipeToElementInHorizontalList(MobileElement element,
                                                   MobileElement horizontalList) {
        for (int i = 0; i < MAX_SWIPES; ++i) {
            if (isDisplayed(element)) return true;
            swipe(horizontalList, Direction.LEFT, SWIPE_DELTA);
        }
        return false;
    }

    private boolean swipeTo(MobileElement element, Direction direction) {
        final int maxSwipes;
        final int delta;

        switch (direction) {
            case UP:
                maxSwipes = MAX_SWIPES;
                delta = SWIPE_DELTA;
                break;
            case DOWN:
                maxSwipes = 2 * MAX_SWIPES;
                delta = -SWIPE_DELTA / 2;  // swipe down can hide menu
                break;
            default:
                throw new AssertionError("Unexpected direction: " + direction);
        }

        for (int i = 0; i < maxSwipes; ++i) {
            if (isDisplayed(element)) {
                if (direction == Direction.UP)
                    checkOverlappingTabbar(element);
                return true;
            }
            swipe(delta);
        }
        return false;
    }

    private void checkOverlappingTabbar(MobileElement element) {
        if (element.getCenter().y > (int) (0.75 * getWindowSize().height))
            swipe(SWIPE_DELTA / 2);
    }

    public void pinch(boolean open, Point pt) {
        final Point center = getWindowCenter();
        Point a0 = pt;
        Point a1 = getAvgPoint(0.7, center, 0.3, pt);
        if (open) {
            Point tmp = a0;
            a0 = a1;
            a1 = tmp;
        }

        final Point b0 = getAvgPoint(2., center, -1., a0);
        final Point b1 = getAvgPoint(2., center, -1., a1);
        final WaitOptions timeout = waitOptions(Duration.ofMillis(200));

        new MultiTouchAction(getDriver())
            .add(newTouchAction().press(point(a0)).waitAction(timeout).moveTo(point(a1)).release())
            .add(newTouchAction().press(point(b0)).waitAction(timeout).moveTo(point(b1)).release())
            .perform();
    }

    private static Point getAvgPoint(double a, Point ptA, double b, Point ptB) {
        return new Point((int) (a * ptA.x + b * ptB.x), (int) (a * ptA.y + b * ptB.y));
    }

    public void rotate() {
        final Point center = getWindowCenter();
        final int dx = center.x / 4, dy = center.x / 2;
        final PointOption<?> topLeft = point(center.moveBy(-dx, -dy));
        final PointOption<?> topRight = point(center.moveBy(dx, -dy));
        final PointOption<?> bottomLeft = point(center.moveBy(-dx, dy));
        final PointOption<?> bottomRight = point(center.moveBy(dx, dy));

        WaitOptions timeout = waitOptions(Duration.ofMillis(500));

        final MobileDriver<MobileElement> driver = getDriver();
        new MultiTouchAction(driver)
            .add(newTouchAction()
                .press(topLeft).waitAction(timeout).moveTo(topRight).release())
            .add(newTouchAction()
                .press(bottomRight).waitAction(timeout).moveTo(bottomLeft).release())
            .perform();
    }

    public TouchAction<?> newTouchAction() {
        return new TouchAction<>(getDriver());
    }

    @Step("Swipe {element} {direction}")
    public void swipe(MobileElement element, Direction direction) {
        final Point pt = element.getCenter();
        final Dimension size = getWindowSize();
        switch (direction) {
            case UP:
                swipe(pt.x, pt.y, pt.x, 0);
                break;
            case DOWN:
                swipe(pt.x, pt.y, pt.x, size.height - 10);
                break;
            default:
                throw new AssertionError("Unexpected direction: " + direction);
        }
    }

    @Step("Swipe {direction}")
    public void swipe(Direction direction) {
        double x0 = 0.5, x1 = 0.5, y0 = 0.5, y1 = 0.5;
        switch (direction) {
            case UP:
                y0 = 0.8;
                y1 = 0.3;
                break;
            case DOWN:
                y0 = 0.3;
                y1 = 0.8;
                break;
            case LEFT:
                x0 = 0.9;
                x1 = 0.1;
                break;
            case RIGHT:
                x0 = 0.1;
                x1 = 0.9;
                break;
            default:
                throw new AssertionError("Unexpected direction: " + direction);
        }

        final Dimension size = getWindowSize();
        swipe((int) (x0 * size.width), (int) (y0 * size.height),
                (int) (x1 * size.width), (int) (y1 * size.height));
    }

    @Step("Swipes {delta}")
    private void swipe(int delta) {
        Dimension windowSize = getWindowSize();
        int centerX = windowSize.width / 2;
        int centerY = windowSize.height / 2;

        swipe(centerX, centerY + delta / 2, centerX, centerY - delta / 2);
    }

    @Step("Swipes {element} to {direction} by {delta}")
    private void swipe(MobileElement element, Direction direction, int delta) {
        Point center = element.getCenter();
        final int halfOfDelta = delta / 2;

        switch (direction) {
            case UP:
                swipe(center.x, center.y + halfOfDelta, center.x, center.y - halfOfDelta);
                break;
            case DOWN:
                swipe(center.x, center.y - halfOfDelta, center.x, center.y + halfOfDelta);
                break;
            case LEFT:
                swipe(center.x + halfOfDelta, center.y, center.x  - halfOfDelta, center.y);
                break;
            case RIGHT:
                swipe(center.x - halfOfDelta, center.y, center.x  + halfOfDelta, center.y);
                break;
            default:
                throw new AssertionError("Unexpected direction: " + direction);
        }
    }

    private void setScreenOrientation(ScreenOrientation orientation) {
        screenOrientation = orientation;
        getDriver().rotate(orientation);
    }

    public final Dimension getWindowSize() {
        return getDriver().manage().window().getSize();
    }

    public final Point getWindowCenter() {
        return getRelativePoint(0.5, 0.5);
    }

    public final Point getRelativePoint(double x, double y) {
        Dimension windowSize = getWindowSize();
        return new Point((int) (windowSize.width * x), (int) (windowSize.height * y));
    }

    public void waitForLog(String substring) {
        waitForLog(substring, Duration.ofSeconds(10));
    }

    @Step("Waiting for {substring} in log")
    public void waitForLog(String substring, Duration timeout) {
        shouldSee(() -> getLogManager().find(substring) != null, timeout, substring + " in log");
    }

    @Step("Looking for {substring} in logging history")
    public void shouldSeeLogInAllLogs(String substring) {
        Assert.assertNotNull(String.format("String '%s' is not displayed in log", substring),
            getLogManager().searchInAllLogs(substring));
    }

    public void readLogs() {
        getLogManager().readLogs();
    }

    static String getOptEnv(String name) {
        final String value = System.getenv(name);
        if (value == null || value.isEmpty())
            return null;
        return value;
    }

    static String getEnv(String name, String defaultValue) {
        final String value = getOptEnv(name);
        if (value == null)
            return defaultValue;
        return value;
    }

    static String getEnv(String name) {
        final String value = getOptEnv(name);
        if (value == null)
            throw new RuntimeException(String.format("Undefined variable 'env.%s'", name));
        return value;
    }
}
