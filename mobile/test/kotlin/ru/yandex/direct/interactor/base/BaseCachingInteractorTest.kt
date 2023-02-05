// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.base

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import ru.yandex.direct.repository.base.BaseLocalRepository
import ru.yandex.direct.repository.base.BaseRemoteRepository

class BaseCachingInteractorTest {
    @Test
    fun getFromLocal_shouldQueryLocalRepo() {
        val interactor = BaseCachingInteractorImpl()
        interactor.localRepo.stub {
            on { select(any()) } doReturn "ok"
        }
        val query = "Show me, what you got!"
        interactor.selectFromLocal(query).subscribe()
        interactor.scheduler.triggerActions()
        verify(interactor.localRepo).select(query)
        verify(interactor.remoteRepo, never()).fetch(query)
    }

    @Test
    fun fetchFromRemote_shouldNotLoadFromLocal() {
        val interactor = BaseCachingInteractorImpl()
        interactor.fetchForced("ok", "ok").subscribe()
        interactor.scheduler.triggerActions()
        verify(interactor.remoteRepo).fetch("ok")
        verify(interactor.localRepo, never()).select("ok")
    }

    @Test
    fun fetchFromRemote_shouldUpdateInLocal() {
        val interactor = BaseCachingInteractorImpl()
        whenever(interactor.remoteRepo.fetch(any())).doReturn("data")
        interactor.fetchForced("ok", "ok").subscribe()
        interactor.scheduler.triggerActions()
        verify(interactor.localRepo).update("ok", "data")
    }

    @Test
    fun fetchForced_shouldFetchFromRemote_andReturnFromLocal() {
        val observer = TestObserver.create<String>()
        val interactor = BaseCachingInteractorImpl()
        whenever(interactor.remoteRepo.fetch(any())).doReturn("remote data")
        whenever(interactor.localRepo.select(any())).doReturn("local data")

        interactor.fetchForced("remote query", "local query").subscribe(observer)
        interactor.scheduler.triggerActions()
        observer.assertValues("local data")

        val inOrder = inOrder(interactor.remoteRepo, interactor.localRepo)
        inOrder.verify(interactor.remoteRepo).fetch("remote query")
        inOrder.verify(interactor.localRepo).update("local query", "remote data")
        inOrder.verify(interactor.localRepo).select("local query")
    }

    @Test
    fun fetchIfAbsent_shouldFetchRemoteRepo_ifLocalIsEmpty() {
        val observer = TestObserver.create<String>()
        val interactor = BaseCachingInteractorImpl()
        whenever(interactor.remoteRepo.fetch(any())).doReturn("remote data")
        whenever(interactor.localRepo.select(any())).doReturn("local data")
        whenever(interactor.localRepo.containsActualData(any())).doReturn(false)

        interactor.fetchIfAbsent("remote query", "local query").subscribe(observer)
        interactor.scheduler.triggerActions()
        observer.assertValues("local data")

        val inOrder = inOrder(interactor.remoteRepo, interactor.localRepo)
        inOrder.verify(interactor.localRepo).containsActualData("local query")
        inOrder.verify(interactor.remoteRepo).fetch("remote query")
        inOrder.verify(interactor.localRepo).update("local query", "remote data")
        inOrder.verify(interactor.localRepo).select("local query")
    }

    @Test
    fun fetchIfAbsent_shouldNotFetchRemoteRepo_ifLocalHasData() {
        val observer = TestObserver.create<String>()
        val interactor = BaseCachingInteractorImpl()
        whenever(interactor.remoteRepo.fetch(any())).doReturn("remote data")
        whenever(interactor.localRepo.select(any())).doReturn("local data")
        whenever(interactor.localRepo.containsActualData(any())).doReturn(true)

        interactor.fetchIfAbsent("remote query", "local query").subscribe(observer)
        interactor.scheduler.triggerActions()
        observer.assertValues("local data")

        verify(interactor.localRepo).containsActualData("local query")
        verify(interactor.localRepo).select("local query")
        verify(interactor.remoteRepo, never()).fetch(any())
        verify(interactor.localRepo, never()).update(eq("local query"), any())
    }

    private class BaseCachingInteractorImpl(
            localRepo: BaseLocalRepository<String, String> = mock(),
            remoteRepo: BaseRemoteRepository<String, String> = mock(),
            val scheduler: TestScheduler = TestScheduler())
        : BaseCachingInteractor<String, String, String>(localRepo, remoteRepo, scheduler, scheduler)
}