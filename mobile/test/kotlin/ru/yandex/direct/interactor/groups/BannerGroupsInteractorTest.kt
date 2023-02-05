// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.groups

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.banners.BannerGroup
import ru.yandex.direct.interactor.banners.BannersInteractor
import ru.yandex.direct.interactor.dict.RegionsInteractor
import ru.yandex.direct.newui.groups.GroupItem
import ru.yandex.direct.repository.groups.BannerGroupsQuery
import ru.yandex.direct.repository.groups.GroupsLocalRepository
import ru.yandex.direct.repository.groups.GroupsRemoteRepository

class BannerGroupsInteractorTest {
    private lateinit var bannersInteractor: BannersInteractor

    private lateinit var remoteRepo: GroupsRemoteRepository

    private lateinit var localRepo: GroupsLocalRepository

    private lateinit var interactor: BannerGroupsInteractor

    private val scheduler = TestScheduler()

    @Before
    fun runBeforeAnyTest() {
        localRepo = mock()
        remoteRepo = mock()
        bannersInteractor = mock {
            on { loadBanners(any(), anyOrNull(), anyOrNull()) } doReturn Single.just(listOf(ApiSampleData.bannerInfo))
            on { loadBannersForced(any(), anyOrNull(), anyOrNull()) } doReturn Single.just(listOf(ApiSampleData.bannerInfo))
            on { updateStateForAllGroupBanners(any(), any()) } doReturn Completable.complete()
        }
        val regionsInteractor = mock<RegionsInteractor> {
            on { allRegionsFromDb } doReturn Single.just(emptyMap())
            on { updateRegionsIfOutdated() } doReturn Completable.complete()
        }
        interactor = BannerGroupsInteractor(localRepo, remoteRepo, scheduler,
                bannersInteractor, mock(), regionsInteractor, scheduler)
    }

    @Test
    fun resume_shouldUpdateGroup_ifBannersWereResumedSuccessfully() {
        val group = ApiSampleData.bannerGroup
        bannersInteractor.stub {
            on { resumeBanners(any()) } doReturn Maybe.just(true)
            on { loadBannersForced(anyOrNull(), anyOrNull(), anyOrNull()) } doReturn Single.just(emptyList())
        }

        interactor.resumeGroup(group).subscribe()
        scheduler.triggerActions()

        verify(bannersInteractor).resumeBanners(group.banners)
        verify(bannersInteractor).loadBannersForced(null, group.id, null)
        verify(remoteRepo).fetch(BannerGroupsQuery.ofSingleGroup(group.id))
    }

    @Test
    fun suspend_shouldUpdateGroup_ifBannersWereSuspendedSuccessfully() {
        val group = ApiSampleData.bannerGroup
        bannersInteractor.stub {
            on { suspendBanners(any()) } doReturn Maybe.just(true)
            on { loadBannersForced(anyOrNull(), anyOrNull(), anyOrNull()) } doReturn Single.just(emptyList())
        }
        remoteRepo.stub {
            on { fetch(any()) } doReturn emptyList<BannerGroup>()
        }

        interactor.suspendGroup(group).subscribe()
        scheduler.triggerActions()

        verify(bannersInteractor).suspendBanners(group.banners)
        verify(bannersInteractor).loadBannersForced(null, group.id, null)
        verify(remoteRepo).fetch(BannerGroupsQuery.ofSingleGroup(group.id))
    }

    @Test
    fun resume_shouldNotUpdateGroup_ifBannersResumeFailed() {
        val group = ApiSampleData.bannerGroup
        bannersInteractor.stub {
            on { resumeBanners(any()) } doReturn Maybe.just(false)
        }

        interactor.resumeGroup(group).subscribe()
        scheduler.triggerActions()

        verify(bannersInteractor).resumeBanners(group.banners)
        verify(bannersInteractor, never()).loadBannersForced(anyOrNull(), anyOrNull(), anyOrNull())
        verify(remoteRepo, never()).fetch(any())
    }

    @Test
    fun suspend_shouldNotUpdateGroup_ifBannersSuspendFailed() {
        val group = ApiSampleData.bannerGroup
        bannersInteractor.stub {
            on { suspendBanners(any()) } doReturn Maybe.just(false)
        }

        interactor.suspendGroup(group).subscribe()
        scheduler.triggerActions()

        verify(bannersInteractor).suspendBanners(group.banners)
        verify(bannersInteractor, never()).loadBannersForced(anyOrNull(), anyOrNull(), anyOrNull())
        verify(remoteRepo, never()).fetch(any())
    }

    @Test
    fun loadGroupsForCampaign_shouldAlsoLoadBanners() {
        val group = ApiSampleData.bannerGroup
        val searchQuery = ""

        val query = BannerGroupsQuery.ofCampaignGroups(group.campaignId, searchQuery)

        bannersInteractor.stub {
            on { loadBanners(group.campaignId, null, null) } doReturn Single.just(listOf(ApiSampleData.bannerInfo))
        }

        localRepo.stub {
            on { containsActualData(any()) } doReturn true
            on { select(any()) } doReturn listOf(group)
        }

        val observer = TestObserver.create<List<GroupItem>>()
        interactor.loadGroupsForCampaign(group.campaignId, searchQuery).subscribe(observer)
        scheduler.triggerActions()

        verify(bannersInteractor).loadBanners(group.campaignId, null, null)
        verify(localRepo).containsActualData(query)
        verify(localRepo).select(query)

        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(observer.values()[0][0].bannerGroup)
                .isEqualToComparingFieldByFieldRecursively(group)
        assertThat(observer.values()[0][0].bannerGroup.banners)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(listOf(ApiSampleData.bannerInfo))
    }
}