package ru.auto.test.common.utils

import ru.auto.data.util.LoadableData
import rx.Observable
import rx.Single

fun <T> Single<T>.wrapToLoadableData(): Single<LoadableData<T>> =
    map<LoadableData<T>> { LoadableData.Success(it) }
        .onErrorReturn { LoadableData.Failure(it) }

fun <T> Observable<T>.wrapToLoadableData(): Observable<LoadableData<T>> =
    map<LoadableData<T>> { LoadableData.Success(it) }
        .onErrorReturn { LoadableData.Failure(it) }
