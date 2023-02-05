package ru.yandex.navi.ui;

import org.junit.Assert;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;
import java.util.List;

public abstract class OverviewScreen extends BaseScreen {
    protected OverviewScreen() {
        super();
    }

    public static OverviewScreen getVisible() {
        NewOverviewScreen overviewScreen = new NewOverviewScreen();
        if (overviewScreen.isDisplayed())
            return overviewScreen;

        return new MapOverviewScreen();
    }

    public static OverviewScreen waitForRoute() {
        final MobileUser user = MobileUser.getUser();
        user.waitForLog("route.show-variants", Duration.ofMinutes(2));
        dismissOfflineCachePopup();
        return OverviewScreen.getVisible();
    }

    private static void dismissOfflineCachePopup() {
        Dialog dialog = new Dialog("^Стройте маршруты без интернета");
        if (dialog.isDisplayed()) {
            dialog.tryClickAt("Закрыть");
        }
    }

    public final void checkBalloons() {
        checkBalloons(true);
    }

    public final void checkBalloons(boolean expectedBalloons) {
        final List<Balloon> balloons = Balloon.getVariantBalloons();

        if (expectedBalloons) {
            Assert.assertFalse("Balloons not found", balloons.isEmpty());
        } else {
            final Balloon balloon = balloons.isEmpty() ? null : balloons.get(0);
            if (balloon != null) {
                Assert.fail(String.format("Unexpected balloon '%s' at %s",
                    balloon.getImageName(), balloon.getCenter()));
            }
        }
    }

    public final void clickGo() {
        doClickGo();

        user.waitForLog("guidance.set_route");
        MapScreen.getVisible().checkPanelEta();
    }

    public final void clickCancel() {
        doClickCancel();
    }

    public SearchScreen clickSearch() {
        doClickSearch();
        return SearchScreen.getVisible();
    }

    protected abstract void doClickGo();
    protected abstract void doClickCancel();
    protected abstract void doClickSearch();
}
