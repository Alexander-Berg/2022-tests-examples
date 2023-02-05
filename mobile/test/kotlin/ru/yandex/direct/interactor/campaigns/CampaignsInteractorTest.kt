// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.campaigns

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.domain.ShortCampaignInfo
import ru.yandex.direct.repository.campaigns.CampaignsLocalRepository
import ru.yandex.direct.repository.campaigns.CampaignsQuery
import ru.yandex.direct.repository.campaigns.CampaignsRemoteRepository

class CampaignsInteractorTest {

    private val apiCampaign = ShortCampaignInfo().apply { campaignId = 0 }

    private val localCampaign = ShortCampaignInfo().apply { campaignId = 1 }

    private lateinit var scheduler: TestScheduler

    private lateinit var observer: TestObserver<ShortCampaignInfo>

    private lateinit var interactor: CampaignsInteractor

    private lateinit var localRepo: CampaignsLocalRepository

    private lateinit var remoteRepo: CampaignsRemoteRepository

    @Before
    fun runBeforeAnyTest() {
        scheduler = TestScheduler()
        observer = TestObserver()
        localRepo = mock { on { select(any()) } doReturn listOf(apiCampaign) }
        remoteRepo = mock { on { fetch(any()) } doReturn listOf(apiCampaign) }
        interactor = CampaignsInteractor(localRepo, remoteRepo, scheduler, scheduler, mock(), mock(), mock(), scheduler)
    }

    @Test
    fun updateDailyBudget_shouldCallRepoToMakeUpdate() {
        interactor.updateDailyBudget(localCampaign).subscribe(observer)
        scheduler.triggerActions()
        verify(remoteRepo).updateDailyBudget(localCampaign)
    }

    @Test
    fun updateDailyBudget_shouldFetchCampaignFromApi() {
        interactor.updateDailyBudget(localCampaign).subscribe(observer)
        scheduler.triggerActions()
        verify(remoteRepo).fetch(CampaignsQuery.ofSingleCampaign(localCampaign.campaignId))
    }

    @Test
    fun updateDailyBudget_shouldSaveCampaignToDb() {
        interactor.updateDailyBudget(localCampaign).subscribe(observer)
        scheduler.triggerActions()
        verify(localRepo).update(CampaignsQuery.ofSingleCampaign(localCampaign.campaignId), listOf(apiCampaign))
    }

    @Test
    fun updateDailyBudget_shouldReturnCampaignFromApi() {
        interactor.updateDailyBudget(localCampaign).subscribe(observer)
        scheduler.triggerActions()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(apiCampaign)
    }
}