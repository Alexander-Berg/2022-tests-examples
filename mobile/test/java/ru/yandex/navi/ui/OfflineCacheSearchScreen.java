package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.junit.Assert;

import java.time.Duration;
import java.util.List;

final class OfflineCacheSearchScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Поиск\")")
    @iOSXCUITFindBy(accessibility = "Поиск")
    private MobileElement view;

    @AndroidFindBy(className = "android.widget.EditText")
    @iOSXCUITFindBy(className = "XCUIElementTypeSearchField")
    private MobileElement textSearch;

    @AndroidFindBy(id = "cache_region_title")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeCell[`visible=1`]")
    private List<MobileElement> items;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @AndroidFindBy(id = "cache_download_button")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeCell/XCUIElementTypeButton")
    private List<MobileElement> buttonsDownload;

    @AndroidFindBy(uiAutomator = ".text(\"Загружено\")")
    @iOSXCUITFindBy(accessibility = "Загружено")
    private MobileElement labelDone;

    private OfflineCacheSearchScreen() {
        super();
        setView(view);
    }

    public static OfflineCacheSearchScreen getVisible() {
        OfflineCacheSearchScreen screen = new OfflineCacheSearchScreen();
        screen.checkVisible();
        return screen;
    }

    void downloadMap(String region) {
        user.types(textSearch, region);

        user.shouldSee("Cache items", items, Duration.ofSeconds(1));
        user.clicks(items.get(0));

        clickDownload();

        Dialog dialog = new Dialog("Нет доступа к Wi-Fi");
        if (dialog.isDisplayed())
            dialog.clickAt("Да");

        user.waitForLog("download-maps.download-map-completed", Duration.ofMinutes(1));
        user.shouldSee(labelDone);
    }

    private void clickDownload() {
        for (MobileElement button : buttonsDownload) {
            if (button.isDisplayed()) {
                button.click();
                return;
            }
        }

        Assert.fail("Button 'download' is not displayed");
    }
}
