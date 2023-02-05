package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.Point;

class ParkingTrafficButton extends BaseScreen {
    @AndroidFindBy(id = "button_parkingtraffic")
    @iOSXCUITFindBy(accessibility = "button_parkingtraffic")
    private MobileElement button;

    ParkingTrafficButton() {
        super();
        setView(button);
    }

    final Point traffic() {
        return getItem(0);
    }

    final Point parking() {
        return getItem(1);
    }

    private Point getItem(int index) {
        Point point = button.getCenter();
        return point.moveBy((index * 2 - 1) * button.getSize().width / 4, 0);
    }
}
