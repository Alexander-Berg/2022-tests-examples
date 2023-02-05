package ru.yandex.market.di.module

import ru.yandex.market.clean.presentation.feature.nativepayment.NativePaymentModule
import ru.yandex.market.clean.presentation.feature.nativepayment.NativePaymentResultMapper
import ru.yandex.market.clean.presentation.feature.nativepayment.PaymentKitFactory
import ru.yandex.market.di.nativepayment.MockNativePaymentResultMapper
import ru.yandex.market.di.nativepayment.MockPaymentKitFactory
import ru.yandex.market.mocks.StateFacade
import ru.yandex.market.mocks.local.mapper.NativePaymentCardsMapper
import javax.inject.Inject

class TestNativePaymentModule @Inject constructor(
    private val stateFacade: StateFacade,
    private val cardsMapper: NativePaymentCardsMapper
) : NativePaymentModule() {

    override fun providePaymentKitFactory(): PaymentKitFactory {
        return MockPaymentKitFactory(stateFacade, cardsMapper)
    }

    override fun providePaymentResultMapper(): NativePaymentResultMapper {
        return MockNativePaymentResultMapper(stateFacade)
    }
}