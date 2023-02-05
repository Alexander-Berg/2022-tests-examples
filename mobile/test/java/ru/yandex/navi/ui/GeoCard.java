package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import io.qameta.allure.Step;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;

import java.time.Duration;

import static io.appium.java_client.touch.WaitOptions.waitOptions;
import static io.appium.java_client.touch.offset.PointOption.point;

public class GeoCard extends BaseScreen {
    @AndroidFindBy(id = "card_header_title")
    private MobileElement title;

    @AndroidFindBy(id = "view_objectcard_root")
    private MobileElement card;

    @AndroidFindBy(id = "image_searchcard_close")
    @iOSXCUITFindBy(accessibility = "image_searchcard_close")
    private MobileElement closeButton;

    @AndroidFindBy(uiAutomator = ".text(\"Поехали\")")
    @iOSXCUITFindBy(accessibility = "Поехали")
    public MobileElement buttonGo;

    @AndroidFindBy(uiAutomator = ".text(\"Отсюда\")")
    @iOSXCUITFindBy(accessibility = "Отсюда")
    public MobileElement buttonFrom;

    @AndroidFindBy(uiAutomator = ".text(\"Заехать\")")
    @iOSXCUITFindBy(accessibility = "Заехать")
    public MobileElement buttonVia;

    @AndroidFindBy(id = "titlesubtitle_right_icon")
    private MobileElement buttonCopyCoordinates;

    public GeoCard() {
        super();
        setView(closeButton);
    }

    public static GeoCard getVisible() {
        return getVisible(Duration.ofSeconds(3));
    }

    public static GeoCard getVisible(Duration timeout) {
        GeoCard card = new GeoCard();
        card.checkVisible(timeout);
        return card;
    }

    @Step("Нажать на 'Добавить'")
    public AddPlacePopup clickAdd() {
        user.clicks("Добавить");
        return AddPlacePopup.getVisible();
    }

    @Step("Нажать на 'Позвонить'")
    public DialActivity clickCall() {
        user.clicks("Позвонить");
        return DialActivity.getVisible();
    }

    @Step("Нажать на 'Сайт'")
    public void clickSite() {
        user.clicks("Сайт");
    }

    @Step("Нажать 'Заехать'")
    public void clickVia() {
        user.clicks(buttonVia);
    }

    @Step("Нажать на 'Поехали'")
    public void clickGo() {
        user.clicks(buttonGo);
    }

    @Step("Нажать 'Оплатить")
    public void clickPay() {
        user.clicks("Оплатить");
    }

    @Step("Нажать 'Сохранить'")
    public AddPlacePopup clickSave() {
        user.clicks("Сохранить");
        return AddPlacePopup.getVisible();
    }

    @Step("Тап по значку 'Копировать координаты'")
    public void clickCopyCoordinates() {
        user.clicks(buttonCopyCoordinates);
    }

    @Step("Закрыть карточку тапом на крестик")
    public void closeGeoCard() {
        user.clicks(closeButton);
        user.shouldNotSee(this);
    }

    public void checkText(String text) {
        user.shouldSee(text);
    }

    public void expectButton(String value) {
        user.shouldSee(user.findElementByText(value));
    }

    public boolean hasButton(String value) {
        return !user.findElementsByText(value).isEmpty();
    }

    @Step("Свернуть карточку в минимальное состояние свайпом вниз")
    public void swipeDownToMinState() {
        final Point from = title.getCenter();
        final Rectangle cardRect = card.getRect();

        user.newTouchAction()
            .longPress(point(from))
            .waitAction(waitOptions(Duration.ofMillis(500)))
            .moveTo(point(from.x, cardRect.y + cardRect.height * 3 / 4))
            .release()
            .perform();
    }

    @Step("Раскрыть карточку свайпом вверх")
    public GeoCard swipeUp() {
        final Point from = title.getCenter();

        user.newTouchAction()
            .longPress(point(from))
            .waitAction(waitOptions(Duration.ofMillis(500)))
            .moveTo(point(from.x, 0))
            .release()
            .perform();

        return this;
    }
}
