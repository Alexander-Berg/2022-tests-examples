package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;
import java.util.List;

public final class GasStationCard extends BaseScreen {
    @AndroidFindBy(id = "tanker_title")
    @iOSXCUITFindBy(accessibility = "Тестировочная АЗС")
    private MobileElement view;

    @AndroidFindBy(id = "background_card")
    private List<MobileElement> items;

    @AndroidFindBy(id = "tankerPayBtn")
    private MobileElement buttonPay;

    @AndroidFindBy(id = "tankerFirstOrderBtn")
    private MobileElement buttonFirstOrder;

    private GasStationCard() {
        super();
        setView(view);
    }

    public static GasStationCard getVisible() {
        GasStationCard screen = new GasStationCard();
        screen.checkVisible();
        return screen;
    }

    @Step("Выбрать любую колонку")
    public GasStationCard clickColumn() {
        user.shouldSee("колонки АЗС", items, Duration.ofSeconds(0));
        user.clicks(items.get(0));
        return this;
    }

    @Step("Click {value}")
    public void click(String value) {
        user.clicks(value);
    }

    @Step("Click button pay")
    public void clickPay() {
        user.clicks(buttonPay);
    }

    public void skipFirstOrder() {
        if (MobileUser.isDisplayed(buttonFirstOrder))
            user.clicks(buttonFirstOrder);
    }
}
