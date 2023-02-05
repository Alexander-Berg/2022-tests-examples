package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.ScreenOrientation;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.FullscreenInputDialog;
import ru.yandex.navi.ui.GeoCard;
import ru.yandex.navi.ui.LongTapMenu;
import ru.yandex.navi.ui.OverviewScreen;
import ru.yandex.navi.ui.RoadEventPanel;

@RunWith(RetryRunner.class)
public final class CommonTest extends BaseTest {
    @Test
    public void launch() {
        rotateAndReturn();
    }

    @Test
    @Ignore("MOBNAVI-23917")
    @Category({BuildCheck.class, UnstableIos.class})
    public void exitAfterGo() {
        GeoCard card = mapScreen.searchAndClickFirstItem("Магадан");
        card.clickGo();
        user.restartActivity();
    }

    @Test
    @Category({BuildCheck.class})
    @TmsLink("navigator-854")  // hash: 0x765769a4
    public void basicInterface() {
        testBasicInterface();
    }

    @Test
    public void voiceControl() {
        dismissPromoBanners();

        mapScreen.clickVoice();
        user.waitForLog("alice_view.open");

        mapScreen.tapToCloseAlice();
        user.waitForLog("alice_view.close");
    }

    @Test
    @Issue("MOBNAVI-18102")
    public void setRoadEvent() {
        RoadEventPanel roadEventPanel = mapScreen.addRoadEvent();
        roadEventPanel.clickComment();

        Dialog dialog = new Dialog("Войдите в аккаунт, чтобы установить дорожное событие");
        if (dialog.isDisplayed()) {  // TODO: check with authorization
            dialog.clickAt("Отмена");
        } else {
            FullscreenInputDialog.getVisible().enterText("Привет").clickDone();
        }

        roadEventPanel.clickClose();
    }

    @Test
    @Category({BuildCheck.class})
    @Issue("MOBNAVI-17267")
    @TmsLink("navigator-851")  // hash: 0x26c3b4fb
    public void orientation() {
        experiments.enable(Experiment.TURN_OFF_GAS_STATIONS_COVID_19_MAP_PROMO).apply();

        LongTapMenu longTapMenu = mapScreen.longTap();

        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        longTapMenu.clickTo();
        OverviewScreen.waitForRoute();

        user.rotatesTo(ScreenOrientation.PORTRAIT);
        OverviewScreen.getVisible().clickGo();

        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        mapScreen.checkPanelEta();
        mapScreen.tapMap();

        user.rotatesTo(ScreenOrientation.PORTRAIT);
        Dialog dialog = mapScreen.clickResetRoute();

        user.rotatesTo(ScreenOrientation.LANDSCAPE);
        user.shouldSee(dialog);
        dialog.clickAt("Да");
        mapScreen.checkPanelEta(false);
    }
}
