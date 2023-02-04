package ru.auto.test.common.utils

import ru.auto.test.BuildConfig

val isBuildTypeDebug
    get() = BuildConfig.BUILD_TYPE == "debug"
