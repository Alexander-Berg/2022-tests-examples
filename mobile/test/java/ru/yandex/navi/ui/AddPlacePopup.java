package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.NoRetryException;
import ru.yandex.navi.tf.Rect;

import java.util.ArrayList;
import java.util.List;

public final class AddPlacePopup extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Добавить место\")")
    @iOSXCUITFindBy(accessibility = "Добавить место")
    private MobileElement view;

    private AddPlacePopup() {
        super();
        setView(view);
    }

    public static AddPlacePopup getVisible() {
        AddPlacePopup popup = new AddPlacePopup();
        popup.checkVisible();
        popup.checkLayout();
        return popup;
    }

    // Issue: MOBNAVI-11211"
    private void checkLayout() {
        try {
            final Rect rectHomeWork = Rect.forItems(findElements("Дом", "Работа"));
            final Rect rectButtonsAdd = Rect.forItems(findElements("Добавить"));
            final Rect rectItems = Rect.forItems(findElements("Избранное", "Новый список"));
            final Rect rectCancel = Rect.forItem(user.findElementByText("Отменить"));

            Assert.assertNotNull(rectHomeWork.top);
            Assert.assertNotNull(rectHomeWork.bottom);
            Assert.assertNotNull(rectHomeWork.left);

            Assert.assertNotNull(rectButtonsAdd.top);
            Assert.assertNotNull(rectButtonsAdd.right);
            Assert.assertNotNull(rectButtonsAdd.bottom);

            Assert.assertNotNull(rectItems.top);
            Assert.assertNotNull(rectItems.bottom);
            Assert.assertNotNull(rectItems.left);

            final double DELTA = user.getWindowSize().width * 0.01;
            Assert.assertEquals(rectButtonsAdd.right, rectCancel.right, 5 * DELTA);

            final int centerX = (rectItems.left + rectButtonsAdd.right) / 2;
            final Point center = user.getWindowCenter();
            Assert.assertEquals(centerX, center.x, DELTA);
        }
        catch (AssertionFailedError err) {
            throw new NoRetryException("Layout error in AddPlacePopup: " + err);
        }
    }

    private List<MobileElement> findElements(String... items) {
        List<MobileElement> result = new ArrayList<>();
        for (String item : items)
            result.addAll(user.findElementsByText(item));
        return result;
    }

    public void saveToFavorites() {
        clickFavorites().clickSave();
    }

    public void saveToFavorites(String name) {
        clickFavorites().enterText(name).clickSave();
    }

    @Step("Нажать 'Дом'")
    public void clickHome() {
        user.clicks("Дом");
    }

    @Step("Нажать 'Избранное'")
    public InputDialog clickFavorites() {
        user.clicks("Избранное");
        return InputDialog.addBookmarkPopup();
    }

    @Step("Нажать '{item}'")
    public InputDialog clickItem(String item) {
        user.clicks(item);

        return InputDialog.withTitle("Название закладки");
    }
}
