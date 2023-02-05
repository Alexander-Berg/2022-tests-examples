package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.util.List;

public class DownloadMapsScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Загрузка карт\")")
    @iOSXCUITFindBy(accessibility = "Загрузка карт")
    private MobileElement view;

    @AndroidFindBy(className = "android.widget.EditText")
    @iOSXCUITFindBy(className = "XCUIElementTypeSearchField")
    private MobileElement textSearch;

    @AndroidFindBy(id = "cache_download_button")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeCell/XCUIElementTypeButton")
    public List<MobileElement> buttonsDownload;

    private DownloadMapsScreen() {
        super();
        setView(view);
    }

    public static DownloadMapsScreen getVisible() {
        DownloadMapsScreen screen = new DownloadMapsScreen();
        screen.checkVisible();
        return screen;
    }

    public void downloadMap(String region) {
        clickSearch().downloadMap(region);
    }

    public void clickRegion(String region) {
        user.findElementByText(region).click();
    }

    private OfflineCacheSearchScreen clickSearch() {
        user.clicks(textSearch);
        return OfflineCacheSearchScreen.getVisible();
    }
}
