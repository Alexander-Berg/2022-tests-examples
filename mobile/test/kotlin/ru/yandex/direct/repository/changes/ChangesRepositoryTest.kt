// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.changes

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import ru.yandex.direct.domain.Changes
import ru.yandex.direct.util.Optional
import ru.yandex.direct.web.api5.result.CheckDictionariesResult
import java.util.*

class ChangesRepositoryTest {
    @Test
    fun getChanges_shouldReturnEmptyResult_ifLocalIsEmpty_RemoteHasNoChanges() {
        TestEnvironment().apply {
            setLocalRepoEmpty()
            setRemoteRepoChangesAt(t2, false)

            val observer = getChanges(this)

            val captor = ArgumentCaptor.forClass(ChangesRemoteQuery::class.java)
            verify(remoteRepo, times(1)).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByField(ChangesRemoteQuery.withEmptyTimestamp())

            assertThat(observer.values())
                    .usingFieldByFieldElementComparator()
                    .containsExactly(ChangesCheckResult(null))
        }
    }

    @Test
    fun getChanges_shouldReturnEmptyResult_ifLocalHasChanges_RemoteHasNoChanges() {
        TestEnvironment().apply {
            setLocalRepoChangesAt(t1)
            setRemoteRepoChangesAt(t2, false)

            val observer = getChanges(this)

            val captor = ArgumentCaptor.forClass(ChangesRemoteQuery::class.java)
            verify(remoteRepo, times(1)).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByField(ChangesRemoteQuery.withTimestamp(t1))

            assertThat(observer.values())
                    .usingFieldByFieldElementComparator()
                    .containsExactly(ChangesCheckResult(null))
        }
    }

    @Test
    fun getChanges_shouldReturnChanges_ifLocalIsEmpty_RemoteHasChanges() {
        TestEnvironment().apply {
            setLocalRepoEmpty()
            setRemoteRepoChangesAt(t2, true)

            val observer = getChanges(this)

            val captor = ArgumentCaptor.forClass(ChangesRemoteQuery::class.java)
            verify(remoteRepo, times(1)).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByField(ChangesRemoteQuery.withEmptyTimestamp())

            assertThat(observer.values())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(ChangesCheckResult(Changes.makeRegion(t2)))
        }
    }

    @Test
    fun getChanges_shouldReturnChanges_ifLocalHasChanges_RemoteHasChanges() {
        TestEnvironment().apply {
            setLocalRepoChangesAt(t1)
            setRemoteRepoChangesAt(t2, true)

            val observer = getChanges(this)

            val captor = ArgumentCaptor.forClass(ChangesRemoteQuery::class.java)
            verify(remoteRepo, times(1)).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByField(ChangesRemoteQuery.withTimestamp(t1))

            assertThat(observer.values())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(ChangesCheckResult(Changes.makeRegion(t2)))
        }
    }

    @Test
    fun getChanges_shouldNeverSaveChangesToLocal() {
        TestEnvironment().apply {
            setLocalRepoEmpty()
            setRemoteRepoChangesAt(t2, true)

            getChanges(this)

            verify(localRepo, never()).update(any(), any())
        }
    }

    @Test
    fun saveChanges_shouldSaveChangesToLocal() {
        TestEnvironment().apply {
            val changes = Changes(0, Changes.Type.REGIONS, t1)
            repo.commitChanges(changes).subscribe()
            scheduler.triggerActions()
            verify(localRepo, times(1)).update(changes.type, Optional.just(changes))
        }
    }

    @Test
    fun getChanges_shouldReturnError_ifRemoteReturnedError() {
        TestEnvironment().apply {
            val throwable = RuntimeException()

            setLocalRepoEmpty()
            whenever(remoteRepo.fetch(any())).doThrow(throwable)

            val observer = getChanges(this)

            assertThat(observer.values()).isEmpty()
            assertThat(observer.errors()).containsExactly(throwable)
        }
    }

    @Test
    fun getChanges_shouldReturnError_ifLocalReturnedError() {
        TestEnvironment().apply {
            val throwable = RuntimeException()
            whenever(localRepo.select(any())).doThrow(throwable)

            val observer = getChanges(this)

            assertThat(observer.values()).isEmpty()
            assertThat(observer.errors()).containsExactly(throwable)
        }
    }

    @Test
    fun getChangesForType_shouldWorkCorrectly() {
        TestEnvironment().apply {
            doReturn(t1).`when`(remoteResult).timestamp

            whenever(remoteResult.areRegionsChanged()).doReturn(false)
            whenever(remoteResult.areTimeZonesChanged()).doReturn(false)
            assertThat(repo.getChangesForType(Changes.Type.REGIONS, remoteResult)).isNull()
            assertThat(repo.getChangesForType(Changes.Type.TIME_ZONES, remoteResult)).isNull()

            whenever(remoteResult.areRegionsChanged()).doReturn(false)
            whenever(remoteResult.areTimeZonesChanged()).doReturn(true)
            assertThat(repo.getChangesForType(Changes.Type.REGIONS, remoteResult)).isNull()
            assertThat(repo.getChangesForType(Changes.Type.TIME_ZONES, remoteResult))
                    .isEqualToComparingFieldByField(Changes(null, Changes.Type.TIME_ZONES, t1))

            whenever(remoteResult.areRegionsChanged()).doReturn(true)
            whenever(remoteResult.areTimeZonesChanged()).doReturn(false)
            assertThat(repo.getChangesForType(Changes.Type.REGIONS, remoteResult))
                    .isEqualToComparingFieldByField(Changes(null, Changes.Type.REGIONS, t1))
            assertThat(repo.getChangesForType(Changes.Type.TIME_ZONES, remoteResult)).isNull()

            whenever(remoteResult.areRegionsChanged()).doReturn(true)
            whenever(remoteResult.areTimeZonesChanged()).doReturn(true)
            assertThat(repo.getChangesForType(Changes.Type.REGIONS, remoteResult))
                    .isEqualToComparingFieldByField(Changes(null, Changes.Type.REGIONS, t1))
            assertThat(repo.getChangesForType(Changes.Type.TIME_ZONES, remoteResult))
                    .isEqualToComparingFieldByField(Changes(null, Changes.Type.TIME_ZONES, t1))
        }
    }

    private fun getChanges(environment: TestEnvironment): TestObserver<ChangesCheckResult> {
        val observer = TestObserver<ChangesCheckResult>()
        environment.repo
                .getChanges(Changes.Type.REGIONS)
                .subscribeOn(scheduler)
                .observeOn(scheduler)
                .subscribe(observer)
        scheduler.triggerActions()
        return observer
    }

    class TestEnvironment {
        private val localResult = mock<Changes>()
        val remoteResult = mock<CheckDictionariesResult>()

        val localRepo = mock<ChangesLocalRepository>()

        val remoteRepo = mock<ChangesRemoteRepository> {
            on { fetch(any()) } doReturn remoteResult
        }

        val repo = ChangesRepository(localRepo, remoteRepo, scheduler, scheduler)

        fun setLocalRepoEmpty() {
            whenever(localRepo.select(any())).thenReturn(Optional.nothing())
        }

        fun setLocalRepoChangesAt(timestamp: Date) {
            whenever(localResult.lastCheckTimestamp).doReturn(timestamp)
            whenever(localRepo.select(any())).doReturn(Optional.just(localResult))
        }

        fun setRemoteRepoChangesAt(timestamp: Date, hasChanges: Boolean) {
            whenever(remoteResult.timestamp).doReturn(timestamp)
            whenever(remoteResult.areRegionsChanged()).doReturn(hasChanges)
            whenever(remoteResult.areTimeZonesChanged()).doReturn(hasChanges)
        }
    }

    companion object {
        val t1 = Date(1)
        val t2 = Date(2)
        val scheduler = TestScheduler()
    }
}