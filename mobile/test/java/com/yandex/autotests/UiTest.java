package com.yandex.autotests;

import com.yandex.autotests.fakes.Ui;
import com.yandex.autotests.runner.device.DeviceJUnit4Runner;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(DeviceJUnit4Runner.class)
public class UiTest {

    @Test
    @TestCaseId(9)
    public void testWaitCheckAndClick() {
        final boolean isVisible = Ui.check("ResourceName", "resourceName", "some_id");
        assertFalse(isVisible);
        final boolean firstWaitVisible =
                Ui.wait("ResourceName", "resourceName", "some_id", TimeUnit.SECONDS.toMillis(5));
        assertFalse(firstWaitVisible);
        final boolean secondWaitVisible =
                Ui.wait("Text", "text", "before", TimeUnit.SECONDS.toMillis(10));
        assertTrue(secondWaitVisible);
        final boolean isVisibleAfterWait = Ui.check("Text", "text", "before");
        assertTrue(isVisibleAfterWait);
        Ui.click("ResourceName", "resourceName", "some_id");
        assertFalse(Ui.check("Text", "text", "before"));
        assertTrue(Ui.check("Text", "text", "after"));
    }
}
