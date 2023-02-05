package ru.yandex.yandexbus.inhouse.domain.route

import ru.yandex.maps.toolkit.datasync.binding.DataSyncEvent
import ru.yandex.maps.toolkit.datasync.binding.SharedData
import ru.yandex.maps.toolkit.datasync.binding.datasync.concrete.route.Route
import ru.yandex.maps.toolkit.datasync.binding.error.DataSyncException
import rx.Completable
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject

internal class TestBookmarksSharedData() : SharedData<Route> {

    private val routesMap = mutableMapOf<String, Route>()
    private val subject: BehaviorSubject<MutableList<Route>> = BehaviorSubject.create(mutableListOf())

    var currentRoutes: List<Route>
        get() = routesMap.values.toList()
        set(value) {
            routesMap.clear()
            routesMap.putAll(value.associate { it.uri to it })
            notifyChange()
        }

    private fun notifyChange() {
        subject.onNext(routesMap.values.toMutableList())
    }

    override fun removeAll(): Completable {
        routesMap.clear()
        notifyChange()
        return Completable.complete()
    }

    override fun remove(model: Route): Completable {
        routesMap.remove(model.uri)
        notifyChange()
        return Completable.complete()
    }

    override fun addOrUpdate(model: Route): Single<Route> {
        routesMap[model.uri] = model
        notifyChange()
        return Single.just(model)
    }

    override fun addOrUpdate(models: MutableList<Route>): Single<MutableList<Route>> {
        models.forEach { addOrUpdate(it) }
        return subject.first().toSingle()
    }

    override fun sync(): Completable = Completable.complete()

    override fun data() = subject

    override fun data(forceSync: Boolean) = subject

    override fun errors(): Observable<DataSyncException> = Observable.empty()

    override fun controlEvents(): Observable<DataSyncEvent> = Observable.empty()
}