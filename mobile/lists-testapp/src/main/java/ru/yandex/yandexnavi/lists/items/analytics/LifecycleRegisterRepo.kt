package ru.yandex.yandexnavi.lists.items.analytics

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor

class LifecycleRegisterRepo : LifecycleRegister {
    private val createdCounterProcessor = BehaviorProcessor.createDefault(0)
    override val createdCount: Flowable<Int>
        get() = createdCounterProcessor.onBackpressureLatest()

    private val bindCounterProcessor = BehaviorProcessor.createDefault(0)
    override val bindCount: Flowable<Int>
        get() = bindCounterProcessor.onBackpressureLatest()

    private val attachedCounterProcessor = BehaviorProcessor.createDefault(0)
    override val attachedCount: Flowable<Int>
        get() = attachedCounterProcessor.onBackpressureLatest()

    override fun createId(): Int {
        val id = createdCounterProcessor.value!!
        createdCounterProcessor.onNext(maxOf(0, createdCounterProcessor.value!! + 1))
        return id
    }

    override fun registerAttach() {
        attachedCounterProcessor.onNext(maxOf(0, attachedCounterProcessor.value!! + 1))
    }

    override fun registerDetach() {
        attachedCounterProcessor.onNext(maxOf(0, attachedCounterProcessor.value!! - 1))
    }

    override fun registerBind() {
        bindCounterProcessor.onNext(maxOf(0, bindCounterProcessor.value!! + 1))
    }
}
