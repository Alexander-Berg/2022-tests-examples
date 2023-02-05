package com.yandex.autotests;

import com.yandex.autotests.fakes.TimeManager;
import com.yandex.autotests.runner.device.DeviceJUnit4Runner;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.yandex.frankenstein.device.DeviceRegistry.device;

@RunWith(DeviceJUnit4Runner.class)
public class TimeTest {

    @TestCaseId(12)
    @Test
    public void testGetUptime() {
        final long difference = Math.abs(device().getUptime() - TimeManager.getUptime());
        assert difference <= 2;
    }
}
