package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

import java.time.Duration;

public final class PayParkingScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Оплата парковки\")")
    @iOSXCUITFindBy(accessibility = "Оплата парковки")
    private MobileElement view;

    @AndroidFindBy(id = "etPhone")
    private MobileElement phone;

    private PayParkingScreen() {
        super();
        setView(view);
    }

    public static PayParkingScreen getVisible() {
        PayParkingScreen screen = new PayParkingScreen();
        screen.checkVisible(Duration.ofSeconds(2));
        return screen;
    }

    @Step("Ввести номер телефона")
    public PayParkingScreen enterPhone() {
        user.types(phone, "1111111111");
        return this;
    }

    @Step("Нажать 'Продолжить'")
    public void clickContinue() {
        user.clicks("Продолжить");
    }
}
