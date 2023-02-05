package com.yandex.mail.tools

import android.accounts.Account
import com.nhaarman.mockito_kotlin.mock
import com.yandex.mail.account.MailProvider
import com.yandex.mail.entity.AccountType
import com.yandex.passport.api.Passport.PASSPORT_ENVIRONMENT_PRODUCTION
import com.yandex.passport.api.Passport.PASSPORT_ENVIRONMENT_TEAM_PRODUCTION
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportEnvironment
import com.yandex.passport.api.PassportUid

class MockPassportAccountWrapper(
    private val uid: Long,
    private val name: String,
    private val type: String,
    private val isLogged: Boolean,
    private val accountType: AccountType,
    private val mailProvider: MailProvider,
    private val isYandexoid: Boolean,
    private val isPdd: Boolean
) : PassportAccount by mock() {

    override fun getUid(): PassportUid {
        val env: PassportEnvironment = if (accountType == AccountType.TEAM) PASSPORT_ENVIRONMENT_TEAM_PRODUCTION else PASSPORT_ENVIRONMENT_PRODUCTION
        return PassportUid.Factory.from(env, uid)
    }

    override fun isYandexoid(): Boolean = isYandexoid

    override fun isAuthorized(): Boolean = isLogged

    override fun getAndroidAccount(): Account {
        return Account(name, type)
    }

    override fun isMailish(): Boolean {
        return accountType == AccountType.MAILISH
    }

    override fun getSocialProviderCode(): String? = mailProvider.socialProviderCode

    override fun isPdd(): Boolean = isPdd
}
