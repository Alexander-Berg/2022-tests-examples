// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.testutils.pageobject

class InvalidScreenException(actualScreenClass: Class<*>) : RuntimeException(
        "Current screen assertion failed. Actual class is ${actualScreenClass.simpleName}"
)