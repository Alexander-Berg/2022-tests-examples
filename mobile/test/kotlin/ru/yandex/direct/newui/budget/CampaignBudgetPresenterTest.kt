// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.budget

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import ru.yandex.direct.R
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.ShortCampaignInfo
import ru.yandex.direct.domain.account.DailyBudget
import ru.yandex.direct.domain.enums.DailyBudgetMode
import ru.yandex.direct.interactor.campaigns.CampaignsInteractor
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.utils.FunctionalTestEnvironment

class CampaignBudgetPresenterTest {
    @Test
    fun presenter_shouldSetupViewCorrectly_whenAttached() {
        with(TestEnvironment()) {
            doAttachView()
            verify(view).showTitle(fragmentTitle)
            verify(view).showBudget(campaign.dailyBudget!!)
            verify(view).setMinBudget(currency.minDailyBudget)
            verify(view).showCurrency(currency)
            verify(view).showSharedAccountWarning(true)
        }
    }

    @Test
    fun presenter_shouldThrow_whenSharedAccountWithoutDailyBudget() {
        with(TestEnvironment()) {
            campaign.dailyBudget = null
            assertThatThrownBy { presenter.setCampaign(campaign) }
                .isExactlyInstanceOf(NullPointerException::class.java)
        }
    }

    @Test
    fun presenter_shouldUpdateAccountBudget_whenButtonClicked() {
        with(TestEnvironment()) {
            val confirmClicks = PublishSubject.create<DailyBudget>()
            view.stub { on { confirmButtonClicks } doReturn confirmClicks }
            val closeClicks = PublishSubject.create<Any>()
            view.stub { on { closeButtonClicks } doReturn closeClicks }
            doAttachView()
            confirmClicks.onNext(campaign.dailyBudget!!)
            scheduler.triggerActions()
            val inOrder = inOrder(view, interactor)
            inOrder.verify(view).showLoading()
            inOrder.verify(interactor).updateDailyBudget(campaign)
            inOrder.verify(view).showOk()
            inOrder.verifyNoMoreInteractions()
        }
    }

    @Test
    fun presenter_shouldShowError_whenCannotUpdateBudget() {
        with(TestEnvironment()) {
            val confirmClicks = PublishSubject.create<DailyBudget>()
            view.stub { on { confirmButtonClicks } doReturn confirmClicks }
            val closeClicks = PublishSubject.create<Any>()
            view.stub { on { closeButtonClicks } doReturn closeClicks }
            interactor.stub { on { updateDailyBudget(any()) } doReturn Single.error(Throwable(errorMessage)) }
            doAttachView()
            confirmClicks.onNext(campaign.dailyBudget!!)
            scheduler.triggerActions()
            verify(view).showLoading()
            verify(view).showError(errorMessage)
        }
    }

    class TestEnvironment : FunctionalTestEnvironment() {
        val fragmentTitle = "fragmentTitle"

        val errorMessage = "errorMessage"

        val currency = ApiSampleData.currency[0]

        val campaign = ShortCampaignInfo().apply {
            name = "campaignName"
            campaignId = 12345
            dailyBudget = DailyBudget().apply {
                amount = 0.0
                mode = DailyBudgetMode.STANDARD
            }
        }

        val interactor = mock<CampaignsInteractor>().stub {
            on { updateDailyBudget(campaign) } doReturn Single.just(campaign)
        }

        val clientInteractor = mock<CurrentClientInteractor>().stub {
            on { currentClientInfo } doReturn Maybe.just(ApiSampleData.clientInfo)
        }

        val presenter = CampaignBudgetPresenter(
            passportInteractor,
            defaultErrorResolution,
            scheduler,
            interactor,
            mock { on { currency } doReturn currency },
            clientInteractor,
            resources
        )

        val view = mock<DailyBudgetView>().stubViewMethods().stub {
            on { confirmButtonClicks } doReturn Observable.empty()
            on { closeButtonClicks } doReturn Observable.empty()
        }

        init {
            resources.stub {
                on { getString(R.string.campaign_daily_budget) } doReturn fragmentTitle
            }
        }

        fun doAttachView() {
            presenter.setCampaign(campaign)
            presenter.attachView(view, null)
            scheduler.triggerActions()
        }
    }
}