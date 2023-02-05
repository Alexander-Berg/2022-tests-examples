package ru.yandex.yandexnavi.lists.items.analytics

import io.reactivex.Flowable

interface LifecycleRegister {
    val createdCount: Flowable<Int>
    val attachedCount: Flowable<Int>
    val bindCount: Flowable<Int>

    fun createId(): Int
    fun registerAttach()
    fun registerDetach()
    fun registerBind()
}
