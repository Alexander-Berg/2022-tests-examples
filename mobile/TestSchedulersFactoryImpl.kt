package ru.yandex.market.rx.schedulers

import io.reactivex.schedulers.Schedulers

class TestSchedulersFactoryImpl : AbstractSchedulersFactory {
    override fun io() = Schedulers.trampoline()

    override fun computation() = Schedulers.trampoline()

    override fun mainThread() = Schedulers.trampoline()
}