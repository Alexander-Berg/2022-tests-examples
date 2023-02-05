package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import ru.yandex.navi.tf.Direction;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public class NewOverviewScreen extends OverviewScreen {
    @AndroidFindBy(id = "view_overview_root")
    @iOSXCUITFindBy(className = "XCUIElementTypePageIndicator")
    private MobileElement view;

    @AndroidFindBy(id = "button_overview_go")
    @iOSXCUITFindBy(accessibility = "Поехали")
    private MobileElement buttonGo;

    @AndroidFindBy(id = "button_overview_cancel")
    @HowToUseLocators(iOSXCUITAutomation = ALL_POSSIBLE)
    @iOSXCUITFindBy(accessibility = "Отмена")
    @iOSXCUITFindBy(accessibility = "Сброс")
    private MobileElement buttonCancel;

    @AndroidFindBy(id = "button_overview_search")
    @iOSXCUITFindBy(accessibility = "button_overview_search")
    private MobileElement buttonSearch;

    @AndroidFindBy(id = "page_control_overview")
    private MobileElement pager;

    NewOverviewScreen() {
        super();
        setView(view);
    }

    @Override
    protected void doClickGo() {
        user.clicks(buttonGo);
    }

    @Override
    protected void doClickCancel() {
        user.clicks(buttonCancel);
    }

    @Override
    protected void doClickSearch() {
        user.clicks(buttonSearch);
    }

    public void swipe(Direction direction) {
        int y = pager.getCenter().y;
        int width = pager.getSize().width;
        final int x0, x1;

        switch (direction) {
            case LEFT: x0 = width * 9 / 10; x1 = 0; break;
            case RIGHT: x0 = 0; x1 = width * 9 / 10; break;
            default: throw new AssertionError("Unexpected direction: " + direction);
        }

        user.swipe(x0, y, x1, y);
    }
}
