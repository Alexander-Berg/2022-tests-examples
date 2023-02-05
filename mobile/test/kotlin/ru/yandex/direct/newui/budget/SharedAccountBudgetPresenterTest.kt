// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.budget

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import ru.yandex.direct.R
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.account.DailyBudget
import ru.yandex.direct.domain.account.management.SharedAccount
import ru.yandex.direct.domain.enums.DailyBudgetMode
import ru.yandex.direct.interactor.account.SharedAccountInteractor
import ru.yandex.direct.utils.FunctionalTestEnvironment

class SharedAccountBudgetPresenterTest {
    @Test
    fun presenter_shouldSetupViewCorrectly_whenAttached() {
        with(TestEnvironment()) {
            doAttachView()
            verify(view).showTitle(sharedAccountTitle)
            verify(view).showBudget(sharedAccount.dailyBudget!!)
            verify(view).setMinBudget(currency.minAccountDailyBudget)
            verify(view).showCurrency(currency)
            verify(view).showSharedAccountWarning(false)
        }
    }

    @Test
    fun presenter_shouldThrow_whenSharedAccountWithoutDailyBudget() {
        with(TestEnvironment()) {
            sharedAccount.dailyBudget = null
            assertThatThrownBy { presenter.setSharedAccount(sharedAccount) }
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
            confirmClicks.onNext(sharedAccount.dailyBudget!!)
            scheduler.triggerActions()
            val inOrder = inOrder(view, interactor)
            inOrder.verify(view).showLoading()
            inOrder.verify(interactor).updateDailyBudget(sharedAccount)
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
            confirmClicks.onNext(sharedAccount.dailyBudget!!)
            scheduler.triggerActions()
            verify(view).showLoading()
            verify(view).showError(errorMessage)
        }
    }

    class TestEnvironment : FunctionalTestEnvironment() {
        val sharedAccountTitle = "sharedAccountTitle"

        val errorMessage = "errorMessage"

        val currency = ApiSampleData.currency[0]

        val sharedAccount = SharedAccount().apply {
            dailyBudget = DailyBudget().apply {
                amount = 0.0
                mode = DailyBudgetMode.STANDARD
            }
        }

        val interactor = mock<SharedAccountInteractor>().stub {
            on { updateDailyBudget(sharedAccount) } doReturn Single.just(sharedAccount)
        }

        val presenter = SharedAccountBudgetPresenter(
            passportInteractor,
            defaultErrorResolution,
            scheduler,
            interactor,
            mock { on { currency } doReturn currency },
            mock()
        )

        val view = mock<DailyBudgetView>().stubViewMethods().stub {
            on { confirmButtonClicks } doReturn Observable.empty()
            on { closeButtonClicks } doReturn Observable.empty()
        }

        init {
            resources.stub {
                on { getString(eq(R.string.shared_account_daily_budget)) } doReturn sharedAccountTitle
            }
        }

        fun doAttachView() {
            presenter.setSharedAccount(sharedAccount)
            presenter.attachView(view, null)
            scheduler.triggerActions()
        }
    }
}