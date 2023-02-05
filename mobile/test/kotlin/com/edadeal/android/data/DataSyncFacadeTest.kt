package com.edadeal.android.data

import com.edadeal.android.data.datasync.DataSyncProvider
import com.edadeal.android.data.datasync.LoyaltyCardsFacadeImpl
import com.edadeal.android.model.DataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(MockitoJUnitRunner::class)
class DataSyncFacadeTest {
    private val databaseId = "dataSyncDb"
    private val scheduler = TestScheduler()
    private val locationDataLoadSubject = PublishSubject.create<DataManager.Result>()

    @Mock private lateinit var dataSyncProvider: DataSyncProvider
    private lateinit var facade: LoyaltyCardsFacadeImpl

    @BeforeTest
    fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { scheduler }

        val syncEvents = Observable.never<DataSyncProvider.SyncInfo>()
        val locationLoadEvents = locationDataLoadSubject.hide()
        facade = LoyaltyCardsFacadeImpl(mock(), databaseId, dataSyncProvider, mock(), syncEvents, locationLoadEvents)
        whenever(dataSyncProvider.updateDatabase(any())).thenReturn(Maybe.empty())
    }

    @Test
    fun `dataSyncFacade should update database on location data load`() {
        locationDataLoadSubject.onNext(DataManager.Result.OK)
        advanceTime()
        verify(dataSyncProvider, times(1)).updateDatabase(databaseId)

        locationDataLoadSubject.onNext(DataManager.Result.OK)
        advanceTime()
        verify(dataSyncProvider, times(2)).updateDatabase(databaseId)

        advanceTime()
        verifyNoMoreInteractions(dataSyncProvider)
    }

    @AfterTest
    fun teardown() {
        RxJavaPlugins.setIoSchedulerHandler(null)
    }

    private fun advanceTime() {
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
    }
}
