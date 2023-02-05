package ru.yandex.market.perftests.scenario

import androidx.test.uiautomator.UiDevice
import com.yandex.perftests.runner.PerfTestUtils

interface MarketPerfTest {

    val packageName: String

    val perfTestUtils: PerfTestUtils

    val device: UiDevice

}