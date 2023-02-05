package ru.yandex.navi.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.MobileUser;

public final class EditViaPointPopup {
    private final MobileUser user;
    private Point buttonAdjust;
    private Point buttonRemove;

    private EditViaPointPopup() {
        this.user = MobileUser.getUser();
    }

    public static EditViaPointPopup getVisible() {
        return new EditViaPointPopup();
    }

    private void update() {
        final Dimension size = user.getWindowSize();
        buttonAdjust = getButton(size, 1);
        buttonRemove = getButton(size, 2);
    }

    private Point getButton(Dimension size, int pos) {
        return new Point(pos * size.width / 5, (int) (size.height / 3.6));
    }

    @Step("Нажать кнопку 'Уточнить'")
    public void clickAdjust() {
        update();
        user.tap("Уточнить", buttonAdjust);
    }

    @Step("Нажать кнопку 'Удалить'")
    public void clickRemove() {
        update();
        user.tap("Удалить", buttonRemove);
    }
}
