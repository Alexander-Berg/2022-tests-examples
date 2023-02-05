package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class PaymentMethodsScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Способы оплаты\")")
    @iOSXCUITFindBy(accessibility = "Способы оплаты")
    private MobileElement view;

    @AndroidFindBy(id = "wallet")
    @iOSXCUITFindBy(accessibility = "Кошелек и привязанные карты")
    private MobileElement wallet;

    private PaymentMethodsScreen() {
        super();
        setView(view);
    }

    public static PaymentMethodsScreen getVisible() {
        PaymentMethodsScreen screen = new PaymentMethodsScreen();
        screen.checkVisible();
        return screen;
    }

    public void clickWallet() {
        user.clicks(wallet);
    }
}
