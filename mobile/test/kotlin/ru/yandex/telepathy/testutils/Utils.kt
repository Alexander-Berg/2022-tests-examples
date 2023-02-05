// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.telepathy.testutils

import ru.yandex.telepathy.model.JsonPath
import java.lang.IllegalArgumentException

fun jsonPath(vararg path: Any): JsonPath {
    return path.fold(JsonPath()) { jsonPath, it ->
        when (it) {
            is String -> jsonPath.plus(it)
            is Int -> jsonPath.plus(it)
            else -> throw IllegalArgumentException(it.toString())
        }
    }
}
