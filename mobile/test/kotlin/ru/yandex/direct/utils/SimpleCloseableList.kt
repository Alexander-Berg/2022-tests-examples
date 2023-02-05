// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import ru.yandex.direct.util.CloseableList


class SimpleCloseableList<T> : ArrayList<T>, CloseableList<T> {
    constructor() : super()

    constructor(source: Collection<T>) : super(source)

    override fun close() {
    }

    override fun isClosed() = false
}
