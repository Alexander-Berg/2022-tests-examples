// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM feature/personal-information-fields-validator-feature.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class PersonalInformationFieldsValidatorFeature private constructor(): Feature<PersonalInformationFieldsValidator>("PersonalInformationFieldsValidatorFeature", "Validate personal information fields") {
    companion object {
        @JvmStatic var `get`: PersonalInformationFieldsValidatorFeature = PersonalInformationFieldsValidatorFeature()
    }
}

public interface PersonalInformationFieldsValidator {
    fun getPhoneNumberErrorText(): String
    fun getEmailErrorText(): String
}

