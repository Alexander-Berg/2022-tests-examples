package ru.yandex.navi.ui;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.qameta.allure.Step;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.Platform;

public final class LongTapMenu {
    private final MobileUser user;
    private Point buttonClose;
    private Point buttonTo;
    private Point buttonVia;
    private Point buttonFrom;
    private Point buttonWhatIsHere;
    private Point buttonToMyPlaces;

    private LongTapMenu() {
        this.user = MobileUser.getUser();
    }

    @CanIgnoreReturnValue
    public static LongTapMenu getVisible() {
        return new LongTapMenu();
    }

    private void update() {
        final Dimension size = user.getWindowSize();

        final int y0, y1, y2, width;

        if (user.getOrientation() == ScreenOrientation.PORTRAIT) {
            width = size.width;

            if (user.getPlatform() == Platform.iOS) {
                y0 = 70;
                y1 = 150;
                y2 = 300;
            } else {
                y0 = size.height / 10;
                y1 = size.height / 4;
                y2 = (int) (size.height / 2.3);
            }
        } else {
            y0 = size.height / 10;
            y1 = size.height / 3;
            y2 = size.height * 2 / 3;

            if (user.getPlatform() == Platform.iOS) {
                width = 465;
            } else {
                width = (int) (size.width / 1.6);
            }
        }

        final int x1 = width / 5;
        this.buttonClose = new Point(x1, y0);
        this.buttonTo = new Point(x1, y1);
        this.buttonVia = new Point(2 * x1, y1);
        this.buttonFrom = new Point(3 * x1, y1);
        this.buttonWhatIsHere = new Point(x1, y2);
        this.buttonToMyPlaces = new Point(3 * x1, y2);
    }

    @Step("Закрыть long-tap меню")
    public void clickClose() {
        update();
        user.tap("Закрыть", buttonClose);
    }

    @Step("Нажать кнопку 'Отсюда'")
    public void clickFrom() {
        update();
        user.tap("Отсюда", buttonFrom);
    }

    @Step("Нажать кнопку 'Сюда'")
    public void clickTo() {
        update();
        user.tap("Сюда", buttonTo);
    }

    @Step("Нажать кнопку 'Через'")
    public void clickVia() {
        update();
        user.tap("Через", buttonVia);
    }

    public GeoCard clickWhatIsHere() {
        update();
        user.tap("Что здесь?", buttonWhatIsHere);

        return GeoCard.getVisible();
    }

    @Step("Нажать кнопку 'В Мои места'")
    public AddPlacePopup clickMyPlaces() {
        update();
        user.tap("В 'Мои места'", buttonToMyPlaces);
        return AddPlacePopup.getVisible();
    }

    public InputDialog clickMyPlacesExpectAddBookmark() {
        update();
        user.tap("В 'Мои места'", buttonToMyPlaces);
        return InputDialog.addBookmarkPopup();
    }
}
