package ru.yandex.market

import ru.yandex.market.clean.domain.antirobot.AntirobotTokenInitializer
import toxin.module

fun robolectricTestOverridesModule() = module {
    factory<AntirobotTokenInitializer>(allowOverride = true) {
        MockAntirobotTokenInitializer()
    }
}