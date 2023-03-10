// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM model/sbp-extended-banks-list-model.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class SbpExtendedBanksListModel: SbpExtendedBanksList {
    private var banks: YSArray<String> = mutableListOf("Sample Bank Ltd.", "OLOLO Bank Ltd.")
    private var query: String = ""
    open override fun getBanksList(): YSArray<String> {
        if (this.query.length == 0) {
            return this.banks
        }
        return this.banks.filter( {
            name ->
            name.includes(this.query)
        })
    }

    open override fun enterSearchQuery(query: String): Unit {
        this.query = query
        return
    }

    open override fun tapOnSearch(): Unit {
        return
    }

    open override fun waitForInterface(mSec: Int): Boolean {
        return true
    }

    open override fun isEmptyMessageDisplayed(): Boolean {
        return this.getBanksList().size == 0
    }

}

