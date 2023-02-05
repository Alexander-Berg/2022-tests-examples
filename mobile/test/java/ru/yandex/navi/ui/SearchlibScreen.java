package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.NoSuchElementException;

import java.time.Duration;

public final class SearchlibScreen extends BaseScreen {
    @AndroidFindBy(id = "button_not_interested")
    @iOSXCUITFindBy(accessibility = "Не надо")
    private MobileElement buttonNegative;

    private SearchlibScreen() {
        super();
        setView(buttonNegative);
    }

    @Step("Close Searchlib splash screen")
    void close() {
        user.clicks(buttonNegative);
        user.shouldNotSee(this);
    }

    public static void dismissIfVisible() {
        try {
            SearchlibScreen screen = new SearchlibScreen();
            screen.user.waitFor(screen, Duration.ofSeconds(2));
            screen.close();
        }
        catch (NoSuchElementException ignore) {
        }
    }
}
