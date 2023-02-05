// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import ru.yandex.direct.newui.error.resolution.ErrorResolution

class TestingErrorResolution : ErrorResolution() {
    override fun resolve(tag: String, throwable: Throwable): Boolean {
        println("${throwable.javaClass.simpleName} with tag <$tag> comes to TestingErrorResolution.")
        println(throwable.message)
        throwable.printStackTrace(System.out)
        return false
    }
}
