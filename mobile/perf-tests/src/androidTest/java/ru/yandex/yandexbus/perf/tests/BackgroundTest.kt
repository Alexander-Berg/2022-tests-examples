package ru.yandex.yandexbus.perf.tests

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.tests.BackgroundTestBase

@PerfTest(
    packageName = PACKAGE,
    description = "Background test",
    owners = ["yauhen"],
    timeoutSeconds = 720,
    defaultRepeat = 3,
    skipFirstResult = false
)
class BackgroundTest : BackgroundTestBase(
    BaseTest(BackgroundTest::class.java),
    initialDelay = 5,
    measurementDuration = 180,
    measurementPeriod = 5
)
