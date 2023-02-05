package ru.yandex.navi.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;

public final class OfflineResultsPanel extends BaseScreen {
    private MapScreen mapScreen;
    private Rectangle panel;

    OfflineResultsPanel(MapScreen mapScreen) {
        super();
        this.mapScreen = mapScreen;
    }

    @Override
    public boolean isDisplayed() {
        update();
        return panel.height > 90;
    }

    @Step("Тапнуть по плашке 'Результаты без интернета'")
    public void click() {
        this.checkVisible();
        user.tap("плашка 'Результаты без интернета'", getCenter(panel));
    }

    @Step("Тапнуть по плашке 'Результаты без интернета'")
    public void tryClick() {
        try {
            if (isDisplayed())
                user.tap("плашка 'Результаты без интернета'", getCenter(panel));
        }
        catch (NoSuchElementException | StaleElementReferenceException e) {
            System.err.println("OfflineResultsPanel.tryClick failed: " + e);
        }
    }

    private static Point getCenter(Rectangle rect) {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    private void update() {
        Rectangle findMeButton = mapScreen.findmeBtn.getRect();
        Rectangle tabBar;
        try {
            tabBar = mapScreen.tabBar.getRect();
        }
        catch (NoSuchElementException e) {
            Dimension window = user.getWindowSize();
            tabBar = new Rectangle(0, window.height, 0, window.width);
        }
        final int y = findMeButton.y + findMeButton.height;
        panel = new Rectangle(tabBar.x, y, tabBar.y - y, tabBar.width);
    }
}
