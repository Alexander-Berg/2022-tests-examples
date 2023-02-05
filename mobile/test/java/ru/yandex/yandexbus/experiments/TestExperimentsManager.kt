package ru.yandex.yandexbus.experiments

import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject

class TestExperimentsManager(private val experiments: MutableMap<String, String> = mutableMapOf()) :
    ExperimentsManager {

    private val active = BehaviorSubject.create<Set<ExperimentGroup>>()
    init {
        notifyActive()
    }

    override fun receivedExperiments(): Observable<Set<ExperimentGroup>> = active
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()

    fun addExperimentGroup(controlGroup: ExperimentGroup) {
        experiments[controlGroup.name] = controlGroup.value
        notifyActive()
    }

    override fun init() {
        // no-op
    }

    override fun current(query: ExperimentQuery): ExperimentGroup? {
        query.precondition?.let {
            if (!it()) {
                return null
            }
        }

        val value = experiments[query.name]
        return value?.let { ExperimentGroup(query.name, it) }
    }

    override fun whenLoaded(query: ExperimentQuery): Single<ExperimentGroup?> {
        return Single.just(current(query))
    }

    private fun notifyActive() {
        active.onNext(experiments.map { ExperimentGroup(it.key, it.value) }.toSet())
    }
}
