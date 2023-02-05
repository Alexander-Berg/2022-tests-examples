package ru.yandex.market

import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.base.presentation.core.mvp.presenter.ReduxPresenterDependencies
import ru.yandex.market.base.presentation.core.schedule.PresentationSchedulers
import ru.yandex.market.base.redux.store.RxAppStateStore
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.feature.money.formatter.FormatSymbolsProvider
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.utils.Characters
import java.math.BigDecimal

fun reduxPresenterDependenciesMock(): ReduxPresenterDependencies<AppState> = ReduxPresenterDependencies(
    appStateStore = RxAppStateStore(
        appStateStore = mock(),
        reduxSchedulers = mock(),
        reduxCommonHealthAnalytics = mock()
    ), speedService = mock(),
    schedulers = presentationSchedulersMock()
)

fun presentationSchedulersMock(): PresentationSchedulers {
    return mock(lenient = true) {
        on { main } doReturn Schedulers.trampoline()
        on { timer } doReturn Schedulers.trampoline()
        on { localSingleThread } doReturn Schedulers.trampoline()
        on { worker } doReturn Schedulers.trampoline()
        on { coroutineMainContext } doReturn Dispatchers.Unconfined
    }
}

fun presentationSchedulersMock(
    stubbing: KStubbing<PresentationSchedulers>.(PresentationSchedulers) -> Unit
): PresentationSchedulers {
    return presentationSchedulersMock().apply { KStubbing(this).stubbing(this) }
}

val Number.rub get() = Money(BigDecimal(toLong()), Currency.RUR)

fun Money.asViewObject() = MoneyVo.builder()
    .amount(amount.value.toString())
    .currency(Characters.RUBLE_SIGN)
    .separator(FormatSymbolsProvider().moneyFormatSymbols.currencySeparator)
    .build()

fun <T> T.mockResult(result: T) {

    whenever(this).thenReturn(result)
}
