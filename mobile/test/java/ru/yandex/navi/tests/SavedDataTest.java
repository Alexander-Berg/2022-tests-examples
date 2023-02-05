package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.SavedDataScreen;

@RunWith(RetryRunner.class)
public final class SavedDataTest extends BaseTest {
    @Test
    @Category({UnstableIos.class})
    public void clearData() {
        SavedDataScreen screen = tabBar.clickMenu().clickSettings().clickSavedData();

        boolean[] confirmations = {false, true};
        for (boolean confirm : confirmations) {
            screen.clickClearSearchHistory(confirm);
            screen.clickClearRouteHistory(confirm);
            screen.clickClearCache(confirm);
        }
    }
}
