// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.uitest

import java.util.concurrent.TimeUnit

object TestSettings {

    const val appPackageId = "ru.yandex.direct.inhouse"

    val launchTimeout = TimeUnit.SECONDS.toMillis(5)

    val longTimeout = TimeUnit.SECONDS.toMillis(10)

    val shortTimeout = TimeUnit.SECONDS.toMillis(2)
}