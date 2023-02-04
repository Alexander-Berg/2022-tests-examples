package com.yandex.mobile.realty.auth

import android.accounts.Account
import android.content.Context
import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportAuthorizationUrlProperties
import com.yandex.passport.api.PassportAutoLoginProperties
import com.yandex.passport.api.PassportFilter
import com.yandex.passport.api.PassportStash
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import com.yandex.passport.api.exception.PassportRuntimeUnknownException
import java.util.*

/**
 * @author matek3022 on 2020-02-04.
 */
class TestAccountService(
    passportStorage: PassportStorage,
    passportFilter: PassportFilter
) : AccountService(passportStorage, passportFilter) {

    private val accountFactory by lazy { AccountFactory() }

    fun setUserAuthorized() {
        val passportUid = PassportUid.Factory.from(Passport.PASSPORT_ENVIRONMENT_TESTING, 1)
        setAuthorized(passportUid)
    }

    fun setAccountData(
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ) {
        accountFactory.email = email
        accountFactory.firstName = firstName
        accountFactory.lastName = lastName
    }

    override fun initializePassportApi(context: Context): PassportApi {
        return TestPassportApi(super.initializePassportApi(context), accountFactory)
    }
}

private class TestPassportApi(
    passportApi: PassportApi,
    private val accountFactory: AccountFactory
) : PassportApi by passportApi {

    override fun getToken(uid: PassportUid): PassportToken {
        return PassportToken { "test" }
    }

    override fun getAccount(uid: PassportUid): PassportAccount {
        return accountFactory.getAccount(uid)
    }

    override fun logout(uid: PassportUid) {}

    override fun tryAutoLogin(autoLoginProperties: PassportAutoLoginProperties): PassportAccount {
        throw PassportRuntimeUnknownException("Skip autologin")
    }

    override fun getAuthorizationUrl(properties: PassportAuthorizationUrlProperties): String {
        return properties.returnUrl
    }
}

private class AccountFactory {

    var email: String? = null
    var firstName: String? = null
    var lastName: String? = null

    fun getAccount(uid: PassportUid): PassportAccount {
        return FakePassportAccount(uid, email, firstName, lastName)
    }

    private class FakePassportAccount(
        private val uid: PassportUid,
        private val email: String?,
        private val firstName: String?,
        private val lastName: String?
    ) : PassportAccount {

        override fun getUid(): PassportUid {
            return uid
        }

        override fun getPrimaryDisplayName(): String {
            return "test"
        }

        override fun getSecondaryDisplayName(): String? {
            return null
        }

        override fun getAvatarUrl(): String? {
            return null
        }

        override fun isAvatarEmpty(): Boolean {
            return true
        }

        override fun getNativeDefaultEmail(): String? {
            return email
        }

        override fun isYandexoid(): Boolean {
            return true
        }

        override fun isBetaTester(): Boolean {
            return false
        }

        override fun isAuthorized(): Boolean {
            return true
        }

        override fun getStash(): PassportStash {
            return PassportStash { null }
        }

        override fun getAndroidAccount(): Account {
            return Account("test", "test")
        }

        override fun isMailish(): Boolean {
            return true
        }

        override fun getSocialProviderCode(): String? {
            return null
        }

        override fun hasPlus(): Boolean {
            return false
        }

        override fun isPhonish(): Boolean {
            return false
        }

        override fun isSocial(): Boolean {
            return false
        }

        override fun isPdd(): Boolean {
            return false
        }

        override fun isLite(): Boolean {
            return false
        }

        override fun getFirstName(): String? {
            return firstName
        }

        override fun getLastName(): String? {
            return lastName
        }

        override fun getBirthday(): Date? {
            return null
        }

        override fun getPublicId(): String? {
            return null
        }
    }
}
