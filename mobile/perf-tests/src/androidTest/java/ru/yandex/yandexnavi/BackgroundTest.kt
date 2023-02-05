package ru.yandex.yandexnavi

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.tests.BackgroundTestBase
import com.yandex.perftests.tests.TestBaseImpl

@PerfTest(
    packageName = naviAppPackage,
    description = "Background test",
    owners = ["scorpy"],
    timeoutSeconds = 720,
    defaultRepeat = 3,
    skipFirstResult = false
)
class BackgroundTest : BackgroundTestBase(
    TestBaseImpl(BackgroundTest::class.java),
    initialDelay = 3,
    measurementDuration = 180,
    measurementPeriod = 5
)
