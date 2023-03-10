// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM feature/payment-screen-title-feature.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class PaymentScreenTitleFeature private constructor(): Feature<PaymentScreenTitle>("PaymentScreenTitleFeature", "Returns current title for payment screen(popup)") {
    companion object {
        @JvmStatic var `get`: PaymentScreenTitleFeature = PaymentScreenTitleFeature()
    }
}

public interface PaymentScreenTitle {
    fun getTitle(): String
}

