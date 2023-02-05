// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.banners

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.banners.ShortBannerInfo
import ru.yandex.direct.repository.banners.BannersLocalRepository
import ru.yandex.direct.repository.banners.BannersQuery
import ru.yandex.direct.repository.banners.BannersRemoteRepository

class BannersInteractorTest {
    private lateinit var localRepository: BannersLocalRepository

    private lateinit var remoteRepository: BannersRemoteRepository

    private lateinit var interactor: BannersInteractor

    private val scheduler = TestScheduler()

    @Before
    fun runBeforeAnyTest() {
        localRepository = mock()
        remoteRepository = mock()
        interactor = BannersInteractor(localRepository, remoteRepository, scheduler, mock(), scheduler)
    }

    @Test
    fun loadBannersForCampaign_shouldWork_likeBaseCachingInteractor() {
        val id = ApiSampleData.campaignInfo.campaignId
        val query = BannersQuery.ofAllCampaignBanners(id, null)
        val result = mutableListOf(ApiSampleData.bannerInfo)

        localRepository.stub {
            on { select(any()) } doReturn result
            on { containsActualData(any()) } doReturn false
        }

        remoteRepository.stub {
            on { fetch(eq(query)) } doReturn result
        }

        val testObserver = TestObserver.create<List<ShortBannerInfo>>()
        interactor.loadBanners(id, null, null).subscribe(testObserver)
        scheduler.triggerActions()

        verify(localRepository).containsActualData(query)
        verify(remoteRepository).fetch(query)
        verify(localRepository).update(query, result)
        verify(localRepository).select(query)

        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun loadBannersForCampaignForced_shouldWork_likeBaseCachingInteractor() {
        val id = ApiSampleData.campaignInfo.campaignId
        val query = BannersQuery.ofAllCampaignBanners(id, null)
        val result = mutableListOf(ApiSampleData.bannerInfo)

        localRepository.stub {
            on { containsActualData(query) } doReturn false
            on { select(query) } doReturn result
        }

        remoteRepository.stub {
            on { fetch(eq(query)) } doReturn result
        }

        val testObserver = TestObserver.create<List<ShortBannerInfo>>()
        interactor.loadBannersForced(id, null, null).subscribe(testObserver)
        scheduler.triggerActions()

        verify(remoteRepository).fetch(query)
        verify(localRepository).update(query, result)
        verify(localRepository).select(query)

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(result)
    }

    @Test
    fun suspendBanners_shouldWork() {
        remoteRepository.stub {
            on { suspend(any()) } doReturn true
        }

        val testObserver = TestObserver.create<Boolean>()
        interactor.suspendBanners(listOf(ApiSampleData.bannerInfo)).subscribe(testObserver)
        scheduler.triggerActions()

        verify(remoteRepository, times(1)).suspend(listOf(ApiSampleData.bannerInfo.id))

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun resumeBanners_shouldWork() {
        remoteRepository.stub {
            on { resume(any()) } doReturn true
        }

        val testObserver = TestObserver.create<Boolean>()
        interactor.resumeBanners(listOf(ApiSampleData.bannerInfo)).subscribe(testObserver)
        scheduler.triggerActions()

        verify(remoteRepository, times(1)).resume(listOf(ApiSampleData.bannerInfo.id))

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }
}