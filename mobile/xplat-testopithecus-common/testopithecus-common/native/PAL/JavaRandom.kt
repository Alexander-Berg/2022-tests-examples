package com.yandex.xplat.testopithecus.common

import java.util.*

object JavaRandom : RandomProvider {
    private val random = Random(0)

    override fun generate(n: Int): Int {
        return random.nextInt(n)
    }
}
