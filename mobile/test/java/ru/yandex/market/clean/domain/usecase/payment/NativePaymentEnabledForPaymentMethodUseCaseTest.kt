package ru.yandex.market.clean.domain.usecase.payment

import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.presentation.feature.payment.NativePaymentEnabledForPaymentMethodUseCase
import ru.yandex.market.common.featureconfigs.managers.GooglePayForceWebToggleManager
import ru.yandex.market.common.featureconfigs.managers.PaymentSdkToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.fintech.usecase.nativepayment.GooglePayForceWebUseCase
import ru.yandex.market.domain.fintech.usecase.nativepayment.NativePaymentEnabledUseCase

@RunWith(Parameterized::class)
class NativePaymentEnabledForPaymentMethodUseCaseTest(
    private val isSdkEnabled: Boolean,
    private val isGooglePayForceWeb: Boolean,
    private val paymentMethod: PaymentMethod,
    private val nativePaymentResult: Boolean
) {

    private val paymentSdkToggleManager = mock<PaymentSdkToggleManager>()
    private val googlePayForceWebToggleManager = mock<GooglePayForceWebToggleManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.googlePayForceWebToggleManager) doReturn googlePayForceWebToggleManager
        whenever(provider.paymentSdkToggleManager) doReturn paymentSdkToggleManager
    }

    private val nativePaymentEnabledUseCase = NativePaymentEnabledUseCase(
        featureConfigsProvider = featureConfigsProvider
    )

    private val googlePayForceWebUseCase = GooglePayForceWebUseCase(
        featureConfigsProvider = featureConfigsProvider
    )

    private val nativePaymentEnabledForPaymentMethodUseCase = NativePaymentEnabledForPaymentMethodUseCase(
        googlePayForceWebUseCase = googlePayForceWebUseCase,
        nativePaymentEnabledUseCase = nativePaymentEnabledUseCase
    )

    @Test
    fun checkUseCasesOutput() {
        whenever(paymentSdkToggleManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isSdkEnabled)))
        whenever(googlePayForceWebToggleManager.getSingle())
            .thenReturn(Single.just(FeatureToggle(isEnabled = isGooglePayForceWeb)))

        nativePaymentEnabledForPaymentMethodUseCase.isNativePaymentEnabledForPaymentMethod(paymentMethod)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue(nativePaymentResult)
    }

    companion object {
        @Parameterized.Parameters(name = "when sdkToggleEnabled is {1} isGooglePayForceWeb is {2} paymentMethod is {3}")
        @JvmStatic
        fun data() = listOf<Array<*>>(
            //0
            arrayOf(
                false, true, PaymentMethod.GOOGLE_PAY, false
            ),
            //1
            arrayOf(
                false, false, PaymentMethod.YANDEX, false
            ),
            //2
            arrayOf(
                true, true, PaymentMethod.GOOGLE_PAY, false
            ),
            //3
            arrayOf(
                true, true, PaymentMethod.YANDEX, true
            ),
            //4
            arrayOf(
                true, false, PaymentMethod.GOOGLE_PAY, true
            )
        )
    }
}