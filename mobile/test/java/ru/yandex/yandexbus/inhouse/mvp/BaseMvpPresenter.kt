package ru.yandex.yandexbus.inhouse.mvp

import ru.yandex.yandexbus.inhouse.mvp.mvp_new.BaseMvpPresenter

fun <V> BaseMvpPresenter<V>.createAttachStart(view: V) {
    onCreate()
    onAttach(view)
    onViewStart()
}