package ru.yandex.market.clean.domain.usecase.payment

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.domain.usecase.payment.googlepay.CheckGooglePayAvailabilityUseCase
import ru.yandex.market.checkout.domain.usecase.payment.googlepay.GooglePayAvailabilityUseCase
import ru.yandex.market.checkout.tds.googlepay.TestGooglePayEnvironment
import ru.yandex.market.clean.data.store.GooglePayAvailabilityDataStore
import ru.yandex.market.domain.fintech.usecase.nativepayment.GooglePayForceWebUseCase
import ru.yandex.market.domain.fintech.usecase.nativepayment.NativePaymentEnabledUseCase
import ru.yandex.market.clean.presentation.feature.oneclick.repository.OneClickRepository
import ru.yandex.market.common.featureconfigs.managers.GooglePayForceWebToggleManager
import ru.yandex.market.common.featureconfigs.managers.PaymentSdkToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.mockResult

@RunWith(Parameterized::class)
class GooglePayAvailabilityUseCaseTest(
    private val isGooglePayDeviceAvailable: Boolean,
    private val isGooglePayPaymentOptionAvailable: Boolean,
    private val isNativePaymentEnabled: Boolean,
    private val isGooglePayForceWeb: Boolean,
    private val result: Boolean,
) {

    private val paymentSdkToggleManager = mock<PaymentSdkToggleManager>()
    private val googlePayForceWebToggleManager = mock<GooglePayForceWebToggleManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.googlePayForceWebToggleManager) doReturn googlePayForceWebToggleManager
        whenever(provider.paymentSdkToggleManager) doReturn paymentSdkToggleManager
    }

    private val checkGooglePayAvailabilityUseCase = mock<CheckGooglePayAvailabilityUseCase>()
    private val googlePayAvailabilityDataStore = mock<GooglePayAvailabilityDataStore>()
    private val oneClickRepository = mock<OneClickRepository>()

    private val nativePaymentEnabledUseCase = NativePaymentEnabledUseCase(
        featureConfigsProvider = featureConfigsProvider
    )

    private val googlePayForceWebUseCase = GooglePayForceWebUseCase(
        featureConfigsProvider = featureConfigsProvider
    )

    private val googlePayAvailabilityUseCase = GooglePayAvailabilityUseCase(
        checkGooglePayAvailabilityUseCase = checkGooglePayAvailabilityUseCase,
        googlePayEnvironment = TestGooglePayEnvironment(),
        googlePayAvailabilityDataStore = googlePayAvailabilityDataStore,
        oneClickRepository = oneClickRepository,
        nativePaymentEnabledUseCase = nativePaymentEnabledUseCase,
        googlePayForceWebUseCase = googlePayForceWebUseCase,
    )

    @Test
    fun checkUseCasesOutput() {
        paymentSdkToggleManager.getSingle()
            .mockResult(Single.just(FeatureToggle(isEnabled = isNativePaymentEnabled)))

        googlePayForceWebToggleManager.getSingle()
            .mockResult(Single.just(FeatureToggle(isEnabled = isGooglePayForceWeb)))

        oneClickRepository.isGooglePayPaymentOptionAvailable()
            .mockResult(Single.just(isGooglePayPaymentOptionAvailable))

        googlePayAvailabilityDataStore.getValue()
            .mockResult(Optional.empty())

        checkGooglePayAvailabilityUseCase.isReadyToPay()
            .mockResult(Single.just(isGooglePayDeviceAvailable))

        googlePayAvailabilityUseCase.isAvailable()
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue(result)
    }

    companion object {
        @Parameterized.Parameters(
            name = "when isGooglePayDeviceAvailable is {1}" +
                    "isGooglePayPaymentOptionAvailable is {2}" +
                    "isNativePaymentEnabled is {3}" +
                    "isGooglePayForceWeb is {4}" +
                    "result is {5}"
        )
        @JvmStatic
        fun data() = listOf<Array<*>>(
            //0
            arrayOf(
                true, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                true //result
            ),
            //1
            arrayOf(
                true, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                false //result
            ),
            //2
            arrayOf(
                true, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                true //result
            ),
            //3
            arrayOf(
                true, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                true //result
            ),
            //4
            arrayOf(
                true, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                true //result
            ),
            //5
            arrayOf(
                true, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                true //result
            ),
            //6
            arrayOf(
                true, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                true //result
            ),
            //7
            arrayOf(
                true, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                true //result
            ),
            //8
            arrayOf(
                false, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                false //result
            ),
            //9
            arrayOf(
                false, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                false //result
            ),
            //10
            arrayOf(
                false, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                false //result
            ),
            //11
            arrayOf(
                false, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                false //result
            ),
            //12
            arrayOf(
                false, //isGooglePayDeviceAvailable
                true, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                false //result
            ),
            //13
            arrayOf(
                false, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                true, //isNativePaymentEnabled
                true, //isGooglePayForceWeb
                false //result
            ),
            //14
            arrayOf(
                false, //isGooglePayDeviceAvailable
                false, //isGooglePayPaymentOptionAvailable
                false, //isNativePaymentEnabled
                false, //isGooglePayForceWeb
                false //result
            )
        )
    }
}