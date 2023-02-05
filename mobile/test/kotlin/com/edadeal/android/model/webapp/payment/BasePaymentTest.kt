package com.edadeal.android.model.webapp.payment

import android.content.Intent
import com.edadeal.android.model.webapp.handler.payment.PaymentMethodData
import com.edadeal.android.model.webapp.handler.payment.PaymentRequest
import com.edadeal.android.model.webapp.handler.payment.PaymentResult
import com.edadeal.android.ui.common.base.ParentUi
import com.yandex.passport.api.PassportUid
import org.mockito.stubbing.Answer

open class BasePaymentTest {
    protected val uid = PassportUid.Factory.from(123L)
    protected val oauthToken = "oauth"
    protected val userEmail = "user@yandex.ru"
    protected val paymentMethod = "yandex"
    protected val paymentData = PaymentMethodData.Data(
        uid = uid.value.toString(),
        userEmail = userEmail,
        oauthToken = oauthToken,
        paymentToken = "abc",
        serviceToken = "service"
    )

    protected fun paymentRequest(
        getResult: (PaymentMethodData) -> PaymentResult = { PaymentResult(it, trust = null) }
    ): Answer<PaymentRequest> = Answer { invocation ->
        val methodData: PaymentMethodData = invocation.getArgument(0)
        object : PaymentRequest {

            override fun dismiss() {}
            override fun startPaymentActivity(parentUi: ParentUi, requestCode: Int) {}

            override fun processPaymentResult(resultCode: Int, data: Intent?): PaymentResult {
                return getResult(methodData)
            }
        }
    }
}
