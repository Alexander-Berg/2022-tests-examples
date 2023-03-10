// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM action/sbp-sample-bank-actions.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class ApproveSbpPurchaseAction(): BaseSimpleAction<SbpSampleBank, MBTComponent>("ApproveSbpPurchaseAction") {
    open override fun performImpl(modelOrApplication: SbpSampleBank, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.clickApprovePurchase()
        return PaymentResultComponent()
    }

    open override fun requiredFeature(): Feature<SbpSampleBank> {
        return SbpSampleBankFeature.`get`
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

}

