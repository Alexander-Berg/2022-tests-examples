package com.edadeal.android.model.webapp.payment

import android.app.Activity
import com.edadeal.android.dto.EnvironmentUserInfo
import com.edadeal.android.model.auth.passport.PassportApiFacade
import com.edadeal.android.model.webapp.handler.payment.PaymentCoordinator
import com.edadeal.android.model.webapp.handler.payment.PaymentError
import com.edadeal.android.model.webapp.handler.payment.PaymentMethodData
import com.edadeal.android.model.webapp.handler.payment.PaymentRequest
import com.edadeal.android.model.webapp.handler.payment.PaymentResult
import com.edadeal.android.ui.common.base.ParentUi
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportToken
import io.reactivex.observers.TestObserver
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(MockitoJUnitRunner::class)
class PaymentCoordinatorTest : BasePaymentTest() {
    @Mock private lateinit var parentUi: ParentUi
    @Mock private lateinit var passportApiFacade: PassportApiFacade
    @Mock private lateinit var paymentRequestFactory: PaymentRequest.Factory

    private lateinit var paymentCoordinator: PaymentCoordinator

    @BeforeTest
    fun prepare() {
        paymentCoordinator = PaymentCoordinator(
            passportApiFacade, { it() }, paymentRequestFactory, { EnvironmentUserInfo.EMPTY }
        )
        paymentCoordinator.onContextAvailable(parentUi)
    }

    @Test
    fun `should return error if payment methods are not supported`() {
        val methodData = PaymentMethodData(supportedMethods = "apple-pay", data = paymentData)
        whenever(paymentRequestFactory.isPaymentMethodSupported("apple-pay")).thenReturn(false)

        val observer = TestObserver<PaymentResult>()
        paymentCoordinator.show(listOf(methodData)).subscribe(observer)

        observer.assertFailsWith(
            errorName = PaymentError.NOT_SUPPORTED_ERROR,
            errorMessage = PaymentError.NO_SUPPORTED_METHOD_FOUND
        )
    }

    @Test
    fun `should use provided payment method data`() {
        val methodData = PaymentMethodData(paymentMethod, paymentData)
        whenever(paymentRequestFactory.isPaymentMethodSupported(paymentMethod)).thenReturn(true)
        whenever(paymentRequestFactory.createPaymentRequest(any())).then(paymentRequest())

        val observer = TestObserver<PaymentResult>()
        paymentCoordinator.show(listOf(methodData)).subscribe(observer)
        observer.assertEmpty()

        paymentCoordinator.onActivityResult(Activity.RESULT_OK, data = null)
        observer.assertResult(methodData = methodData)
    }

    @Test
    fun `should add current user info to payment method data`() {
        val methodData = PaymentMethodData(
            supportedMethods = paymentMethod,
            data = paymentData.copy(uid = null, oauthToken = null, userEmail = null)
        )
        val token = mock<PassportToken>()
        val account = mock<PassportAccount>()
        whenever(token.value).thenReturn(oauthToken)
        whenever(account.nativeDefaultEmail).thenReturn(userEmail)
        whenever(passportApiFacade.getCurrentUid()).thenReturn(uid)
        whenever(passportApiFacade.getToken(eq(uid), any())).thenReturn(token)
        whenever(passportApiFacade.getAccount(eq(uid))).thenReturn(account)
        whenever(paymentRequestFactory.isPaymentMethodSupported(paymentMethod)).thenReturn(true)
        whenever(paymentRequestFactory.createPaymentRequest(any())).then(paymentRequest())

        val observer = TestObserver<PaymentResult>()
        paymentCoordinator.show(listOf(methodData)).subscribe(observer)
        observer.assertEmpty()

        paymentCoordinator.onActivityResult(Activity.RESULT_OK, data = null)
        observer.assertResult(methodData = PaymentMethodData(paymentMethod, paymentData))
    }

    private fun TestObserver<PaymentResult>.assertResult(methodData: PaymentMethodData) {
        assertComplete()
        assertValue { value ->
            value.methodData == methodData && value.trust == null
        }
    }

    private fun TestObserver<PaymentResult>.assertFailsWith(errorName: String, errorMessage: String) {
        assertError { it is PaymentError && it.errorName == errorName && it.errorMessage == errorMessage }
    }
}
