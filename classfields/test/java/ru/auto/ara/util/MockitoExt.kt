package ru.auto.ara.util

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.anyVararg
import org.mockito.stubbing.OngoingStubbing
import ru.auto.ara.util.android.StringsProvider
import ru.auto.data.model.User
import ru.auto.data.repository.user.IUserRepository
import rx.Completable
import rx.Observable
import rx.Single

fun StringsProvider.stubIt() {
    Mockito.`when`(get(ArgumentMatchers.anyInt())).thenReturn("some string value")
    Mockito.`when`(get(ArgumentMatchers.anyInt(), anyVararg())).thenReturn("some string value")
}

fun IUserRepository.stubIt() {
    Mockito.`when`(observeUser()).thenReturn(Observable.just(User.Unauthorized))
}

fun OngoingStubbing<Completable>.thenCompletable() = thenReturn(Completable.complete())

fun <T> OngoingStubbing<Single<T>>.thenSingle(value: T) = thenReturn(Single.just(value))
