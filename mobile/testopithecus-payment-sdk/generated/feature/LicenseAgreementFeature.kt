// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM feature/license-agreement-feature.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class LicenseAgreementFeature private constructor(): Feature<LicenseAgreement>("LicenseAgreement", "License agreement at bottom of an screen and separate License agreement screen") {
    companion object {
        @JvmStatic var `get`: LicenseAgreementFeature = LicenseAgreementFeature()
    }
}

public interface LicenseAgreement {
    fun getLicenseAgreement(): String
    fun isLicenseAgreementShown(): Boolean
    fun openFullLicenseAgreement(): Unit
    fun closeFullLicenseAgreement(): Unit
    fun getFullLicenseAgreement(): String
}

