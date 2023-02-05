package com.yandex.mail.am

import com.yandex.mail.LoginData
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportStashCell
import com.yandex.passport.api.PassportUid

fun PassportApi.savePin(passportAccount: PassportAccount, pinValue: String?) {
    stashValue(passportAccount.uid, PassportStashCell.CELL_MAIL_PIN_CODE, pinValue)
}

fun PassportApi.savePin(uid: Long, pinValue: String?) {
    stashValue(PassportUid.Factory.from(uid), PassportStashCell.CELL_MAIL_PIN_CODE, pinValue)
}

fun PassportApi.getAccount(loginData: LoginData): PassportAccount {
    return getAccount(PassportUid.Factory.from(loginData.uid))
}

fun PassportApi.removeAccount(loginData: LoginData) {
    return removeAccount(PassportUid.Factory.from(loginData.uid))
}

fun Long.toPassportUid(): PassportUid {
    return PassportUid.Factory.from(this)
}
