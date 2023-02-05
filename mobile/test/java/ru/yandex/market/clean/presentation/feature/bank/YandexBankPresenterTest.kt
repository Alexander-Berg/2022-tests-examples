package ru.yandex.market.clean.presentation.feature.bank

import io.reactivex.Completable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.YandexBankAnalytics
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthParams
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthResult
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthTargetScreen
import ru.yandex.market.clean.presentation.navigation.ResultListener
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.fintech.data.YandexBankAccountState
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.presentationSchedulersMock
import java.math.BigDecimal

internal class YandexBankPresenterTest {

    private val router = mock<Router>()
    private val bankAccountStateFlow = MutableStateFlow<YandexBankAccountState?>(null)
    private val useCases = mock<YandexBankUseCases> {
        on { getBankAccountFlow() } doReturn bankAccountStateFlow.filterNotNull()
        on { syncYandexCardPaymentOption() } doReturn Completable.complete()
        on { canShowBankSdk() } doReturn true
    }
    private val actionsMapper = mock<YandexBankActionsMapper>()
    private val healthFacade = mock<YandexBankHealthFacade>()
    private val analyticsFacade = mock<YandexBankAnalytics>()
    private val viewState = mock<`YandexBankView$$State`>()

    private val presenter = YandexBankPresenter(
        presentationSchedulersMock(),
        router,
        useCases,
        actionsMapper,
        healthFacade,
        analyticsFacade
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `show Content only on first view attach`() {
        presenter.attachView(viewState)
        presenter.attachView(viewState)
        verify(viewState).showContent()
    }

    @Test
    fun `go back if bank sdk disabled`() {
        whenever(useCases.canShowBankSdk()) doReturn false
        presenter.attachView(viewState)

        verify(router).backWithResult(YandexBankResult(emptyList()))
    }

    @Test
    fun `navigate to auth request without auto login`() {
        presenter.requestAuthorization()

        val expectedLoginTarget = RequestAuthTargetScreen(RequestAuthParams(tryAutoLogin = false))
        verify(router).navigateForResult(
            eq(expectedLoginTarget),
            argThat { _ -> true }
        )
    }

    @Test
    fun `notify auth success`() {
        presenter.requestAuthorization()

        var resultListener: ResultListener? = null
        verify(router).navigateForResult(
            any(),
            argThat { actualListener ->
                resultListener = actualListener
                true
            }
        )

        resultListener?.onResult(RequestAuthResult(true))
        verify(viewState).onAuthSuccess()
    }

    @Test
    fun `notify auth failed`() {
        presenter.requestAuthorization()

        var resultListener: ResultListener? = null
        verify(router).navigateForResult(
            any(),
            argThat { actualListener ->
                resultListener = actualListener
                true
            }
        )

        resultListener?.onResult(RequestAuthResult(false))
        verify(viewState).onAuthFailed(argThat { error ->
            error is IllegalArgumentException && error.message.equals("authorization failed or canceled")
        })
    }

    @Test
    fun `navigate back with completed actions on view closed`() {
        presenter.attachView(viewState)
        val states = listOf(
            YandexBankAccountState.Unauthenticated,
            YandexBankAccountState.NotCreated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.ONE)),
            YandexBankAccountState.Available(Money.createRub(BigDecimal.TEN))
        )
        val actions = listOf(
            YandexBankResult.Action.AuthorizationSuccess,
            YandexBankResult.Action.AccountCreated,
            YandexBankResult.Action.WalletTopUp
        )
        actions.forEachIndexed { index, action ->
            whenever(actionsMapper.mapFromStateChanges(states[index], states[index + 1])) doReturn action
        }
        states.forEach { bankAccountState ->
            bankAccountStateFlow.value = bankAccountState
        }

        presenter.goBack()
        verify(router).backWithResult(YandexBankResult(actions))
    }

    @Test
    fun `skip account state downgrade for completed actions`() {
        presenter.attachView(viewState)
        val states = listOf(
            YandexBankAccountState.Unauthenticated,
            YandexBankAccountState.NotCreated,
            YandexBankAccountState.Unauthenticated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.ONE)),
            YandexBankAccountState.Unauthenticated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.TEN))
        )
        val actions = listOf(
            YandexBankResult.Action.AuthorizationSuccess,
            YandexBankResult.Action.AccountCreated,
            YandexBankResult.Action.WalletTopUp
        )
        whenever(actionsMapper.mapFromStateChanges(states[0], states[1])) doReturn actions[0]
        whenever(actionsMapper.mapFromStateChanges(states[1], states[3])) doReturn actions[1]
        whenever(actionsMapper.mapFromStateChanges(states[3], states[5])) doReturn actions[2]
        states.forEach { bankAccountState ->
            bankAccountStateFlow.value = bankAccountState
        }

        presenter.goBack()
        verify(router).backWithResult(YandexBankResult(actions))
    }

    @Test
    fun `actualize yandex card payment method on account creation`() {
        presenter.attachView(viewState)
        val states = listOf(
            YandexBankAccountState.NotCreated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.ONE))
        )
        whenever(
            actionsMapper.mapFromStateChanges(
                states[0],
                states[1]
            )
        ) doReturn YandexBankResult.Action.AccountCreated
        states.forEach { bankAccountState ->
            bankAccountStateFlow.value = bankAccountState
        }

        verify(useCases).syncYandexCardPaymentOption()
    }

    @Test
    fun `send health event when authorization failed`() {
        presenter.requestAuthorization()

        var resultListener: ResultListener? = null
        verify(router).navigateForResult(
            any(),
            argThat { actualListener ->
                resultListener = actualListener
                true
            }
        )
        resultListener?.onResult(RequestAuthResult(false))

        verify(healthFacade).sendAuthorizationFailed()
    }

    @Test
    fun `send health event when sdk unavailable`() {
        presenter.attachView(viewState)
        val state = YandexBankAccountState.Unavailable("test")
        bankAccountStateFlow.value = state

        verify(healthFacade).sendYandexBankUnavailable("test")
    }

    @Test
    fun `send health event when account Limited`() {
        presenter.attachView(viewState)
        val state = YandexBankAccountState.Limited
        bankAccountStateFlow.value = state

        verify(healthFacade).sendYandexBankLimited()
    }

    @Test
    fun `send health event when sdk unauthenticated`() {
        presenter.attachView(viewState)
        val state = YandexBankAccountState.Unauthenticated
        bankAccountStateFlow.value = state

        verify(healthFacade).sendYandexBankUnauthenticated()
    }

    @Test
    fun `send health event if bank opened when sdk disabled`() {
        whenever(useCases.canShowBankSdk()) doReturn false
        presenter.attachView(viewState)

        verify(healthFacade).sendYandexBankSdkDisabled()
    }

    @Test
    fun `send visible metric only on first view attach`() {
        presenter.attachView(viewState)
        presenter.attachView(viewState)
        verify(analyticsFacade).yandexBankVisible()
    }

    @Test
    fun `send authorized metric on auth success`() {
        presenter.attachView(viewState)
        val states = listOf(
            YandexBankAccountState.Unauthenticated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.ONE))
        )
        whenever(
            actionsMapper.mapFromStateChanges(
                states[0],
                states[1]
            )
        ) doReturn YandexBankResult.Action.AuthorizationSuccess
        states.forEach { bankAccountState ->
            bankAccountStateFlow.value = bankAccountState
        }

        verify(analyticsFacade).yandexBankAuthSuccess()
    }

    @Test
    fun `send account created metric on card created`() {
        presenter.attachView(viewState)
        val states = listOf(
            YandexBankAccountState.NotCreated,
            YandexBankAccountState.Available(Money.createRub(BigDecimal.ONE))
        )
        whenever(
            actionsMapper.mapFromStateChanges(
                states[0],
                states[1]
            )
        ) doReturn YandexBankResult.Action.AccountCreated
        states.forEach { bankAccountState ->
            bankAccountStateFlow.value = bankAccountState
        }

        verify(analyticsFacade).yandexBankAccountCreated()
    }
}
