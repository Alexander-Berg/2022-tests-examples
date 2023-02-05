package ru.yandex.navi.ui;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.tf.Platform;

public final class MapOverviewScreen extends OverviewScreen {
    private final Point buttonGo;
    private final Point buttonCancel;

    MapOverviewScreen() {
        super();

        final ScreenOrientation orientation = user.getOrientation();
        final Dimension size = user.getWindowSize();
        final int width, y;

        if (user.getPlatform() == Platform.iOS) {
            width = (orientation == ScreenOrientation.PORTRAIT ? size.width : size.height);
            y = (orientation == ScreenOrientation.PORTRAIT ? 115 : 15);
        } else {
            width = size.width;
            y = (orientation == ScreenOrientation.PORTRAIT ? 350 : 150)
                    * Math.max(size.width, size.height) / 1800;
        }

        final int x;
        if (orientation == ScreenOrientation.PORTRAIT)
            x = width / 4;
        else
            x = width * 2 / 5;

        this.buttonGo = new Point(x, y);
        this.buttonCancel = new Point(width - x, y);
    }

    @Override
    protected void doClickGo() {
        user.tap("Поехали", buttonGo);
    }

    @Override
    protected void doClickCancel() {
        user.tap("Отмена", buttonCancel);
    }

    @Override
    protected void doClickSearch() {
        new TabBar().clickSearch();
    }
}
