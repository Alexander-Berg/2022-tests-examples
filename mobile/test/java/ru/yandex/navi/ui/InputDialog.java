package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;

public final class InputDialog extends BaseScreen {
    private String title;

    @AndroidFindBy(id = "edit_input")
    private MobileElement name;

    private InputDialog(String title) {
        super();
        this.title = title;
    }

    static InputDialog withTitle(String title) {
        InputDialog dialog = new InputDialog(title);
        dialog.checkVisible();
        return dialog;
    }

    static InputDialog addBookmarkPopup() {
        return withTitle("Название закладки");
    }

    @Override
    public boolean isDisplayed() {
        return user.findElementByText(title).isDisplayed();
    }

    @Step("Нажать 'Сохранить'")
    public void clickSave() {
        user.clicks("Сохранить");
    }

    @Step("Ввести текст '{text}'")
    public InputDialog enterText(String text) {
        user.types(name, text);
        return this;
    }
}
