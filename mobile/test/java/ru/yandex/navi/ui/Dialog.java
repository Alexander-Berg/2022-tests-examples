package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.qameta.allure.Step;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;

public final class Dialog extends BaseScreen {
    public final String title;

    public Dialog(String title) {
        super();
        this.title = title;
    }

    public static Dialog withTitle(String title) {
        Dialog dialog = new Dialog(title);
        dialog.checkVisible();
        return dialog;
    }

    public static Dialog newAnrAlert() {
        return new Dialog("^Приложение \"Яндекс.Навигатор\" не отвечает");
    }

    @Override
    public boolean isDisplayed() {
        try {
            user.findElementByRegex(title);
            return true;
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    @Step("Нажать '{button}' в диалоге")
    public void clickAt(String button) {
        user.clicks(findButton(button));
    }

    @Step("Нажать '{button}' в диалоге")
    public void tryClickAt(String button) {
        try {
            findButton(button).click();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            System.err.println(String.format("tryClickAt('%s') failed: %s", button, e));
        }
    }

    private MobileElement findButton(String name) {
        return user.findElementByTextIgnoreCase(name);
    }
}
