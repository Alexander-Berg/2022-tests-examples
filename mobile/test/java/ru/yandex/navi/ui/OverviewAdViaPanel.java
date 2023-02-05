package ru.yandex.navi.ui;

import org.junit.Assert;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

public final class OverviewAdViaPanel extends BaseScreen {
    @AndroidFindBy(id = "text_via_title")
    @iOSXCUITFindBy(accessibility = "text_via_title")
    private MobileElement view;

    @AndroidFindBy(id = "switch_via_selectionstatus")
    @iOSXCUITFindBy(accessibility = "switch_via_selectionstatus")
    private MobileElement switchControl;

    private OverviewAdViaPanel() {
        super();
        setView(view);
    }

    public static OverviewAdViaPanel getVisible() {
        OverviewAdViaPanel overviewAdViaPanel = new OverviewAdViaPanel();
        overviewAdViaPanel.checkVisible();
        return overviewAdViaPanel;
    }

    @Step("Переключить свитчер")
    public void clickSwitcher() {
        user.shouldSee(switchControl);

        final boolean isSelectedBeforeClick = isSelected();

        user.clicks(switchControl);

        user.waitForLog("overview-ads.pin-swapped");

        final boolean isViaSelected = isSelected();

        Assert.assertNotEquals("Switch state has not changed after the click",
            isSelectedBeforeClick,
            isViaSelected);

        if (isViaSelected) {
            user.waitForLog("mapkit.search.logger.billboard.click");
        }
    }

    public boolean isSelected() {
        return switchControl.getAttribute("checked").equals("true");
    }

    public String getTitle() {
        return view.getText();
    }
}
