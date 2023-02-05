package com.yandex.vanga

import org.hamcrest.core.Is

fun <T> equalTo(value: T) = Is.`is`(value)