@file:Suppress("VarCouldBeVal")
package com.edadeal.android.model.webapp.payment

import android.app.Activity
import com.edadeal.android.dto.EnvironmentUserInfo
import com.edadeal.android.model.auth.passport.PassportApiFacade
import com.edadeal.android.model.webapp.handler.payment.PaymentCoordinator
import com.edadeal.android.model.webapp.handler.payment.PaymentError
import com.edadeal.android.model.webapp.handler.payment.PaymentHandler
import com.edadeal.android.model.webapp.handler.payment.PaymentMethodData
import com.edadeal.android.model.webapp.handler.payment.PaymentRequest
import com.edadeal.android.model.webapp.handler.payment.PaymentResult
import com.edadeal.android.ui.common.base.ParentUi
import com.edadeal.android.util.adapter
import com.edadeal.platform.JsonRepresentable
import com.edadeal.platform.JsonValue
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportToken
import io.reactivex.Maybe
import io.reactivex.observers.TestObserver
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(MockitoJUnitRunner::class)
class PaymentHandlerTest : BasePaymentTest() {
    @Mock private lateinit var parentUi: ParentUi
    @Mock private lateinit var passportApiFacade: PassportApiFacade
    @Mock private lateinit var paymentRequestFactory: PaymentRequest.Factory

    private lateinit var paymentCoordinator: PaymentCoordinator
    private lateinit var paymentHandler: PaymentHandler

    @BeforeTest
    fun prepare() {
        paymentCoordinator = PaymentCoordinator(
            passportApiFacade, { it() }, paymentRequestFactory, { EnvironmentUserInfo.EMPTY }
        )
        paymentHandler = PaymentHandler(moshi, paymentCoordinator)
        paymentCoordinator.onContextAvailable(parentUi)
    }

    @Test
    fun `should propagate error from the payment coordinator`() {
        val methodData = PaymentMethodData(paymentMethod, paymentData.copy(uid = null, userEmail = null))
        val paymentError = PaymentError(PaymentError.NOT_SUPPORTED_ERROR, PaymentError.EMAIL_NOT_FOUND)
        whenever(paymentRequestFactory.isPaymentMethodSupported(paymentMethod)).thenReturn(true)
        whenever(paymentRequestFactory.createPaymentRequest(methodData)).then { throw paymentError }
        whenever(passportApiFacade.getCurrentUid()).thenReturn(null)

        val observer = TestObserver<JsonRepresentable>()
        paymentHandler.invoke(listOf(methodData)).subscribe(observer)

        observer.assertComplete()
        observer.assertValue { value ->
            val error = value.to<ErrorResponse>().error
            error.name == paymentError.errorName && error.message == paymentError.errorMessage
        }
    }

    @Test
    fun `should return actual method data used for payment`() {
        val methodData = PaymentMethodData(
            supportedMethods = paymentMethod,
            data = paymentData.copy(
                uid = null,
                oauthToken = null,
                userEmail = null
            )
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

        val observer = TestObserver<JsonRepresentable>()
        paymentHandler.invoke(listOf(methodData)).subscribe(observer)
        observer.assertEmpty()

        paymentCoordinator.onActivityResult(Activity.RESULT_OK, data = null)
        observer.assertResult(methodData = PaymentMethodData(paymentMethod, paymentData))
    }

    private fun PaymentHandler.invoke(methods: List<PaymentMethodData>): Maybe<out JsonRepresentable> {
        val params = paramsAdapter.toJson(PaymentHandler.Params(methodData = methods))
        return invoke("payment.showUi", JsonValue(params))
    }

    private fun TestObserver<JsonRepresentable>.assertResult(methodData: PaymentMethodData) {
        assertComplete()
        assertValue { value ->
            val result = value.to<Response>().result
            result.methodData == methodData && result.trust == null
        }
    }

    @JsonClass(generateAdapter = true)
    class Response(
        val result: PaymentResult
    )

    @JsonClass(generateAdapter = true)
    class ErrorResponse(
        val error: Error
    ) {

        @JsonClass(generateAdapter = true)
        class Error(
            val name: String,
            val message: String
        )
    }

    private companion object {
        val moshi: Moshi = Moshi.Builder().build()
        val paramsAdapter: JsonAdapter<PaymentHandler.Params> = moshi.adapter()

        inline fun <reified Response> JsonRepresentable.to(): Response {
            return moshi.adapter<Response>().fromJson(toJson()) ?: throw JsonDataException()
        }
    }
}
