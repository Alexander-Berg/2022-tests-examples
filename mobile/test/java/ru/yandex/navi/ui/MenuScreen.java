package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.Direction;

import java.time.Duration;

public final class MenuScreen extends BaseScreen {
    @AndroidFindBy(id = "menu_screen_view")
    @iOSXCUITFindBy(accessibility = "MenuView")
    private MobileElement view;

    @AndroidFindBy(uiAutomator = ".text(\"Меню\")")
    @iOSXCUITFindBy(accessibility = "Меню")
    public MobileElement title;

    @AndroidFindBy(id = "login_button")
    @iOSXCUITFindBy(accessibility = "Войти")
    public MobileElement buttonLogin;

    @AndroidFindBy(uiAutomator = ".text(\"Выйти\")")
    @iOSXCUITFindBy(accessibility = "Выйти")
    public MobileElement buttonLogout;

    @AndroidFindBy(uiAutomator = ".text(\"Настройки\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Настройки\"`]")
    private MobileElement settings;

    @AndroidFindBy(id = "settings_button")
    @iOSXCUITFindBy(accessibility = "menu_header_settings_button")
    public MobileElement settingsButton;

    @AndroidFindBy(id = "new_user_add_car_button")
    @iOSXCUITFindBy(accessibility = "menu_car_info_add_car_button")
    public MobileElement addCarButton;

    @AndroidFindBy(uiAutomator = ".textStartsWith(\"Яндекс Заправки\")")
    @iOSXCUITFindBy(iOSClassChain =
        "**/XCUIElementTypeStaticText[`name BEGINSWITH \"Яндекc Заправки\"`]")
    public MobileElement gas;

    @AndroidFindBy(uiAutomator = ".text(\"Загрузка карт\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Загрузка карт\"`]")
    public MobileElement downloadMaps;

    @AndroidFindBy(uiAutomator = ".text(\"Обратная связь\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Обратная связь\"`]")
    public MobileElement feedback;

    @AndroidFindBy(uiAutomator = ".text(\"Парковка\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Парковка\"`]")
    public MobileElement parking;

    @AndroidFindBy(uiAutomator = ".text(\"Штрафы ГИБДД\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Штрафы ГИБДД\"`]")
    public MobileElement fines;

    @AndroidFindBy(uiAutomator = ".text(\"О программе\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"О программе\"`]")
    public MobileElement about;

    @AndroidFindBy(id = "navbar_close_button")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar/XCUIElementTypeButton")
    private MobileElement buttonClose;

    @AndroidFindBy(id = "banner_view")
    public MobileElement adv;

    @AndroidFindBy(id = "services_list")
    @iOSXCUITFindBy(accessibility = "menu_services_list")
    public MobileElement servicesList;

    private MenuScreen() {
        super();
        setView(view);
    }

    public static MenuScreen getVisible() {
        MenuScreen screen = new MenuScreen();
        screen.checkVisible(Duration.ofSeconds(2));
        return screen;
    }

    public void checkCentered(MobileElement element) {
        user.shouldSee(element);

        final double DELTA = user.getWindowSize().width * 0.01;
        final Point center = view.getCenter();
        Assert.assertEquals(element.getCenter().x, center.x, DELTA);
    }

    public SettingsScreen clickSettings() {
        user.clicks(MobileUser.isDisplayed(settingsButton) ? settingsButton : settings);
        return SettingsScreen.getVisible();
    }

    @Step("Тапнуть на пункт 'О программе'")
    public AboutScreen clickAbout() {
        user.shouldSeeInScrollable(servicesList);
        user.clickInHorizontalList(about, servicesList);
        return AboutScreen.getVisible();
    }

    public DownloadMapsScreen clickDownloadMaps() {
        user.shouldSeeInScrollable(servicesList);
        user.clickInHorizontalList(downloadMaps, servicesList);
        return DownloadMapsScreen.getVisible();
    }

    public FinesScreen clickFines() {
        user.shouldSeeInScrollable(servicesList);
        user.clickInHorizontalList(fines, servicesList);
        return FinesScreen.getVisible();
    }

    public FeedbackScreen clickFeedback() {
        user.shouldSeeInScrollable(servicesList);
        user.clickInHorizontalList(feedback, servicesList);
        return FeedbackScreen.getVisible();
    }

    @Step("Тапнуть на кнопку 'Войти'")
    public LoginScreen clickLogin() {
        user.clicksInScrollable(buttonLogin);
        return LoginScreen.getVisible();
    }

    public LoginDialog clickLoginExpectLoginDialog() {
        user.clicksInScrollable(buttonLogin);
        return LoginDialog.getVisible();
    }

    public void clickLogout() {
        user.clicksInScrollable(buttonLogout);
    }

    public ParkingScreen clickParking() {
        user.shouldSeeInScrollable(servicesList);
        user.clickInHorizontalList(parking, servicesList);
        user.waitForLog("parking.screen.settings");
        return ParkingScreen.getVisible();
    }

    @Step("Тапнуть пункт '{item}'")
    public void click(String item) {
        user.clicks(item, Direction.DOWN);
    }

    public void close() {
        user.clicks(buttonClose);
    }

    @Step("Выполнить скролл Меню вниз")
    public MenuScreen scrollDown() {
        user.swipe(Direction.UP);
        return this;
    }

    @Step("Выполнить скролл Меню вверх")
    public void scrollUp() {
        user.swipe(Direction.DOWN);
        this.checkVisible();
    }
}
