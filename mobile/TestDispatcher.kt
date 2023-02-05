package com.yandex.pal

object TestDispatcher : Dispatcher {
    override fun <T> async(bg: () -> T, ui: (T) -> Unit) = ui(bg())
    override fun bg(command: () -> Unit) = command()
    override fun ui(command: () -> Unit) = command()

    fun on() {
        Dispatcher.INSTANCE = TestDispatcher
    }

    fun off() {
        Dispatcher.INSTANCE = WorkDispatcher
    }
}

