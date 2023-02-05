package ru.yandex.market

import androidx.annotation.CheckResult
import io.reactivex.Completable
import ru.yandex.market.clean.domain.antirobot.AntirobotTokenInitializer

class MockAntirobotTokenInitializer : AntirobotTokenInitializer {

    @CheckResult
    override fun initialize(): Completable {
        return Completable.complete()
    }
}