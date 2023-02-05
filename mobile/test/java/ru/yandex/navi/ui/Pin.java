package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import ru.yandex.navi.NaviTheme;
import ru.yandex.navi.tf.Displayable;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;

import static io.appium.java_client.touch.WaitOptions.waitOptions;
import static io.appium.java_client.touch.offset.PointOption.point;

public final class Pin implements Displayable {
    private final MobileUser user;
    private final String pinType;
    private final MobileElement element;
    private static NaviTheme naviTheme;

    private Pin(String pinType) {
        this(pinType, null);
    }

    private Pin(String pinType, NaviTheme theme) {
        final String imageName = pinType + (theme == NaviTheme.NIGHT ? "_night" : "");

        this.user = MobileUser.getUser();
        this.pinType = pinType;
        this.element = user.findElementByImage(imageName);
        System.err.println(String.format("Pin '%s' at %s", pinType, element.getCenter()));
    }

    @Override
    public boolean isDisplayed() {
        return MobileUser.isDisplayed(this.element);
    }

    public static void getAltBalloonPin(String icon) {
        new Pin("pin_alt_" + icon);
    }

    public static Pin getAuxPin() {
        return new Pin("pin_aux");
    }

    public static Pin getHomePin() {
        return new Pin("pin_home", getNaviTheme());
    }

    public static Pin getFinishPin() {
        // return new Pin("route_finish");

        // FIXME (kazhoyan): cannot get finish pin with current resource in the project. New icon is
        // a combination of two icons, and it's difficult to combine them so that they match exactly
        // with pin's real icon.
        throw new RuntimeException("FIXME: not implemented");
    }

    public static Pin getParkingPin() {
        return new Pin("parking_380");
    }

    public static Pin getPlacePin() {
        return new Pin("pin_place", getNaviTheme());
    }

    public static Pin getPoiPin() {
        final String[] poiPins = {
            "poi_eat", "poi_eat_green", "poi_bank", "poi_bar", "poi_cafe_day", "poi_cafe_night"
        };
        for (String pinType : poiPins) {
            try {
                return new Pin(pinType);
            } catch (NoSuchElementException e) {
                System.err.println(String.format("Pin %s not found", pinType));
            }
        }

        throw new NoSuchElementException("Poi pin not found");
    }

    public static Pin getRoadEventPin() {
        return new Pin("roadevent_pin_reconstruction");
    }

    public static Pin getSearchPin() {
        return new Pin("pin_search_blue");
    }

    public static Pin getViaPin() {
        return new Pin("pin_via");
    }

    public static Pin getWhatIsHerePin() {
        return new Pin("pin_search_green");
    }

    public static void checkPinHome(boolean visible) {
        try {
            getHomePin();
            Assert.assertTrue(visible);
        }
        catch (NoSuchElementException e) {
            Assert.assertFalse(visible);
        }
    }

    @Step("Тап pin")
    public void tap() {
        user.tap(pinType, element.getCenter());
    }

    @Step("Long-tap pin")
    public void longTap() {
        user.longTap(pinType, element.getCenter());
    }

    @Step("Лонг-тап pin")
    public void longTap(Duration duration) {
        user.longTap(pinType, element.getCenter(), duration);
    }

    public void longTapAndMoveTo(double x, double y) {
        stepLongTapAndMoveTo(element.getCenter(), user.getRelativePoint(x, y));
    }

    @Step("Лонг-тап и перетащить pin {from} -> {to}")
    private void stepLongTapAndMoveTo(Point from, Point to) {
        user.newTouchAction()
            .waitAction(waitOptions(Duration.ofSeconds(1)))
            .press(point(from))
            .waitAction(waitOptions(Duration.ofSeconds(1)))
            .moveTo(point(to))
            .release()
            .perform();
    }

    private static NaviTheme getNaviTheme() {
        if (naviTheme == null)
            naviTheme = new MapScreen().getTheme();
        return naviTheme;
    }
}
