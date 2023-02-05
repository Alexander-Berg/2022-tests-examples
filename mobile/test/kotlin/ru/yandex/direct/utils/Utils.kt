// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import ru.yandex.direct.web.api5.request.BaseGet
import java.text.SimpleDateFormat
import java.util.*

private val mDateFormat = SimpleDateFormat("dd-MM-yyyy").apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun date(dateString: String): Date = mDateFormat.parse(dateString)

@Suppress("UNCHECKED_CAST")
fun <T : BaseGet.BaseParams> baseGetOf() = BaseGet::class.java as Class<BaseGet<T>>

fun <T> List<T>.toCloseable() = SimpleCloseableList(this)

fun <T> emptyCloseableList() = SimpleCloseableList<T>()
