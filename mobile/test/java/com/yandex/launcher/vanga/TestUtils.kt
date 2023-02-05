package com.yandex.launcher.vanga

import org.hamcrest.core.Is

internal fun <T> equalTo(value: T) = Is.`is`(value)