package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.Displayable;
import ru.yandex.navi.tf.MobileUser;

public class MapObject implements Displayable {
    private final MobileUser user;
    private final String imageName;
    private final MobileElement element;

    MapObject(String imageName, MobileElement image) {
        this.user = MobileUser.getUser();
        this.imageName = imageName;
        this.element = image == null ? user.findElementByImage(imageName) : image;
        System.err.println(String.format("MapObject '%s' at %s", imageName, element.getCenter()));
    }

    @Override
    public final boolean isDisplayed() {
        return MobileUser.isDisplayed(this.element);
    }

    public final Point getCenter() {
        return element.getCenter();
    }

    final String getImageName() { return imageName; }

    public void tap() {
        user.tap(imageName, element.getCenter());
    }
}
