package ru.yandex.market

import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportUid
import ru.yandex.market.passport.model.AuthAccount
import ru.yandex.market.passport.model.AuthAutoLoginResult

object PassportTestData {

    fun getPassportUid(value: Long = 42L): PassportUid {
        return PassportUid.Factory.from(
            Passport.PASSPORT_ENVIRONMENT_PRODUCTION,
            value
        )
    }

    fun getPassportAutoLoginResult(
        account: AuthAccount = AuthAccount.testInstance(),
        isDialogRequired: Boolean = false
    ): AuthAutoLoginResult {
        return AuthAutoLoginResult(account, isDialogRequired)
    }
}