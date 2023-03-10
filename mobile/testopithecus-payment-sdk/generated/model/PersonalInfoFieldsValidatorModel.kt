// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM model/personal-info-fields-validator-model.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class PersonalInfoFieldsValidatorModel(private val personalInformationModel: PersonalInformationModel): PersonalInformationFieldsValidator {
    private var phoneNumber: String = ""
    private var phoneNumberErrorShown: Boolean = false
    private var email: String = ""
    private var emailErrorShown: Boolean = false
    open override fun getEmailErrorText(): String {
        this.emailErrorShown = this.isEmailValidationErrorShown()
        return if (this.emailErrorShown) PersonalInformationValidationError.email.toString() else ""
    }

    open override fun getPhoneNumberErrorText(): String {
        this.phoneNumberErrorShown = this.isPhoneNumberValidationErrorShown()
        return if (this.phoneNumberErrorShown) PersonalInformationValidationError.phoneNumber.toString() else ""
    }

    private fun isPhoneNumberValid(): Boolean {
        val phoneNumber = this.personalInformationModel.getFieldValue(PersonalInformationField.phoneNumber)
        return (phoneNumber.length == 0 || (this.allCharDigit(phoneNumber) && (((phoneNumber.startsWith("7") || phoneNumber.startsWith("8")) && phoneNumber.length == 11) || (phoneNumber.startsWith("+7") && phoneNumber.length == 12))))
    }

    private fun allCharDigit(str: String): Boolean {
        return str.split("").filter( {
            item ->
            isCharDigit(item) || item == "+"
        }).size == str.length
    }

    private fun isPhoneNumberValidationErrorShown(): Boolean {
        val phoneNumber = this.personalInformationModel.getFieldValue(PersonalInformationField.phoneNumber)
        if (this.phoneNumberErrorShown && this.phoneNumber == phoneNumber) {
            return true
        }
        this.phoneNumber = phoneNumber
        val focusedField = this.personalInformationModel.getFocusedField()
        val isPhoneNumberValid = this.isPhoneNumberValid()
        return !isPhoneNumberValid && focusedField != PersonalInformationField.phoneNumber
    }

    private fun isEmailValid(): Boolean {
        val email = this.personalInformationModel.getFieldValue(PersonalInformationField.email).split("")
        return (email.size == 0 || (email.contains(".") && email.size >= 5 && email.filter( {
            item ->
            item == "@"
        }).size == 1))
    }

    private fun isEmailValidationErrorShown(): Boolean {
        val email = this.personalInformationModel.getFieldValue(PersonalInformationField.email)
        if (this.emailErrorShown && this.email == email) {
            return true
        }
        this.email = email
        val focusedField = this.personalInformationModel.getFocusedField()
        val isEmailValid = this.isEmailValid()
        return !isEmailValid && focusedField != PersonalInformationField.email
    }

    open fun resetFields(): Unit {
        this.emailErrorShown = false
        this.email = ""
        this.phoneNumberErrorShown = false
        this.phoneNumber = ""
    }

}

public enum class PersonalInformationValidationError(val value: String) {
    phoneNumber("Enter your phone number in the format +70123456789"),
    email("Enter an email address in the format mail@example.com"),
    ;
    override fun toString(): String = value
}
