package ru.yandex.yandexnavi.lists.items.analytics

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers

data class LifecycleMeta(
    val created: Int,
    val binded: Int,
    val attached: Int
)

class CompositeLifecycleRegisterObserver(
    vararg dashboards: LifecycleRegister
) {
    private val dashboards = dashboards.toList()

    fun meta(): Flowable<LifecycleMeta> {
        val created = Flowable.combineLatest(
            dashboards.map(LifecycleRegister::createdCount).toTypedArray()
        ) { input: Array<out Any> ->
            input.map { it as Int }.sum()
        }

        val binded = Flowable.combineLatest(
            dashboards.map(LifecycleRegister::bindCount).toTypedArray()
        ) { input: Array<out Any> ->
            input.map { it as Int }.sum()
        }

        val attached = Flowable.combineLatest(
            dashboards.map(LifecycleRegister::attachedCount).toTypedArray()
        ) { input: Array<out Any> ->
            input.map { it as Int }.sum()
        }

        return Flowables.combineLatest(
            created,
            binded,
            attached
        ) { created, binded, attached ->
            LifecycleMeta(created, binded, attached)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }
}
