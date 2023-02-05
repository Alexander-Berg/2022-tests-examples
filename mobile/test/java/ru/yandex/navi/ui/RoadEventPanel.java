package ru.yandex.navi.ui;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.Platform;

import java.time.Duration;

public class RoadEventPanel {
    private final MobileUser user;

    private final Point textComment;
    private final Point buttonClose;

    private RoadEventPanel() {
        this.user = MobileUser.getUser();

        final Dimension size = user.getWindowSize();
        final int yTitle;
        final int yComment;

        if (user.getPlatform() == Platform.iOS) {
            yTitle = 55;
            yComment = 165;
        } else {
            yTitle = size.height / 10;
            yComment = (int) (size.height / 3.5);
        }

        textComment = new Point(size.width / 2, yComment);
        buttonClose = new Point(size.width * 2 / 10, yTitle);
    }

    public static RoadEventPanel getVisible() {
        RoadEventPanel panel = new RoadEventPanel();
        MobileUser.getUser().waitFor(Duration.ofSeconds(1));
        return panel;
    }

    public final void clickComment() {
        user.tap("Комментарий к дор. событию", textComment);
    }

    public final void clickClose() {
        user.tap("Закрыть", buttonClose);
    }
}
