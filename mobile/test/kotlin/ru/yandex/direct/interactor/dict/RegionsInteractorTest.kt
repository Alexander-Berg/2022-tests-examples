// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.dict

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import ru.yandex.direct.domain.Changes
import ru.yandex.direct.domain.RegionInfo
import ru.yandex.direct.repository.changes.ChangesCheckResult
import ru.yandex.direct.repository.changes.ChangesRepository
import ru.yandex.direct.repository.dicts.RegionsLocalQuery
import ru.yandex.direct.repository.dicts.RegionsLocalRepository
import ru.yandex.direct.repository.dicts.RegionsRemoteQuery
import ru.yandex.direct.repository.dicts.RegionsRemoteRepository
import java.util.*

class RegionsInteractorTest {
    @Test
    fun loadRegionsOnStartup_shouldLoadRegions_ifHasChanges() {
        TestEnvironment().apply {
            changesRepo.stub {
                on { getChanges(any()) } doReturn Single.just(changesCheckResult)
            }
            localRepo.stub {
                on { containsActualData(any()) } doReturn true
            }

            loadRegionsOnStartup(this)

            val captor = ArgumentCaptor.forClass(RegionsRemoteQuery::class.java)
            verify(remoteRepo, times(1)).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByFieldRecursively(RegionsRemoteQuery.ofAllRegions())

            verify(localRepo, times(1)).update(RegionsLocalQuery.ofAllRegions(), emptyList())
            verify(changesRepo, times(1)).getChanges(Changes.Type.REGIONS)
            verify(changesRepo, times(1)).commitChanges(changes)
        }
    }

    private fun loadRegionsOnStartup(environment: TestEnvironment) {
        environment.apply {
            interactor.updateRegionsIfOutdated().subscribeOn(scheduler).observeOn(scheduler).subscribe()
            scheduler.triggerActions()
        }
    }

    class TestEnvironment {
        val changes = Changes(null, Changes.Type.REGIONS, Date(1))
        val changesCheckResult = ChangesCheckResult(changes)

        val remoteRepo = mock<RegionsRemoteRepository> {
            on { fetch(any()) } doReturn emptyList<RegionInfo>()
        }

        val localRepo = mock<RegionsLocalRepository>()
        val changesRepo = mock<ChangesRepository>()

        val scheduler = TestScheduler()

        val interactor = RegionsInteractor(localRepo, remoteRepo, changesRepo, scheduler, scheduler)
    }
}
