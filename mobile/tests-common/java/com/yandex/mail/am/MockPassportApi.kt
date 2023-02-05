package com.yandex.mail.am

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.yandex.mail.LoginData
import com.yandex.mail.entity.AccountType
import com.yandex.mail.fakeserver.FakeServer
import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportAuthorizationUrlProperties
import com.yandex.passport.api.PassportEnvironment
import com.yandex.passport.api.PassportFilter
import com.yandex.passport.api.PassportLoginProperties
import com.yandex.passport.api.PassportStash
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import com.yandex.passport.api.exception.PassportAccountNotAuthorizedException
import com.yandex.passport.api.exception.PassportAccountNotFoundException

class MockPassportApi : PassportApi by EmptyMockPassportApi() {
    private val addedAccounts = ArrayList<LoginData>()

    private val extraDataMap = HashMap<LoginData, HashMap<String, String>>()

    private val xTokens = HashMap<LoginData, String>()

    val addAccountRequests = ArrayList<Int>()

    private val registrationRequests = ArrayList<Int>()

    private var currentAccountName: String? = null

    private var receiveNextPush = false

    override fun removeAccount(uid: PassportUid) {
        addedAccounts.removeIf { it.uid == uid.value }
    }

    fun removeAccount(loginData: LoginData) {
        removeAccount(PassportUid.Factory.from(loginData.uid))
    }

    override fun getToken(uid: PassportUid): PassportToken {
        val loginData = FakeServer.getInstance().getAccountWrapperByUid(uid.value).loginData
        val token = loginData.token
        return if (token != null && loginData.name != AUTH_ERROR_ACCOUNT_NAME) {
            PassportToken { token }
        } else {
            throw PassportAccountNotAuthorizedException()
        }
    }

    fun removeAllAccounts() {
        addedAccounts.clear()
    }

    fun addAccountsToPassport(vararg accounts: LoginData) {
        addedAccounts.addAll(accounts)
    }

    fun setAccountXToken(account: Account, context: Context, xtoken: String) {
        val androidAM = AccountManager.get(context)
        androidAM.setPassword(account, xtoken)
    }

    fun receiveNextPush(receiveNextPush: Boolean) {
        this.receiveNextPush = receiveNextPush
    }

    override fun onPushMessageReceived(from: String, data: Bundle): Boolean {
        val receiveNextPush = this.receiveNextPush
        this.receiveNextPush = false
        return receiveNextPush
    }

    override fun getAccount(passportUid: PassportUid): PassportAccount {
        val loginDataInPassport = addedAccounts.find { loginData -> loginData.uid == passportUid.value }
            ?: throw PassportAccountNotFoundException(passportUid)
        return MockPassportAccount(loginDataInPassport)
    }

    override fun getAccounts(filter: PassportFilter): List<PassportAccount> {
        return addedAccounts.map { MockPassportAccount(it) }
    }

    // TODO authstub ridiculously complex
    override fun stashValue(uid: PassportUid, cell: String, value: String?) {
        val data = extraDataMap.keys.find { it.uid == uid.value }
        if (data != null) {
            val map = extraDataMap[data]!!
            if (value != null) {
                map[cell] = value
            } else {
                map.remove(cell)
            }
        } else if (value != null) {
            val addedAccount = addedAccounts.find { it.uid == uid.value }
            if (addedAccount != null) {
                val hashMap = HashMap<String, String>()
                hashMap[cell] = value
                extraDataMap[addedAccount] = hashMap
            }
        }
    }

    override fun stashValue(uids: MutableList<PassportUid>, cell: String, value: String?) {
        uids.forEach { passportUid: PassportUid -> stashValue(passportUid, cell, value) }
    }

    @SuppressLint("CheckResult")
    override fun getAuthorizationUrl(properties: PassportAuthorizationUrlProperties): String {
        getToken(properties.uid) // just to throw exception if smth goes wrong
        return AUTH_URL
    }

    override fun createLoginIntent(context: Context, loginProperties: PassportLoginProperties): Intent {
        return Intent(RELOGIN_ACTION)
    }

    override fun onInstanceIdTokenRefresh() {
        // noop
    }

    override fun dropToken(token: String) {
        // noop
    }

    override fun setCurrentAccount(uid: PassportUid) {
        // noop
    }

    /**
     * partially implemented PassportAccount with emulation of used funcionality in tests
     */
    inner class MockPassportAccount(private val loginData: LoginData) : PassportAccount by EmptyMockPassportAccount {

        override fun getUid(): PassportUid {
            return object : PassportUid {

                override fun getEnvironment(): PassportEnvironment {
                    return when (AccountType.fromStringType(loginData.type)) {
                        AccountType.TEAM -> Passport.PASSPORT_ENVIRONMENT_TEAM_PRODUCTION
                        AccountType.LOGIN, AccountType.MAILISH -> Passport.PASSPORT_ENVIRONMENT_PRODUCTION
                        else -> Passport.PASSPORT_ENVIRONMENT_PRODUCTION
                    }
                }

                override fun getValue(): Long {
                    return loginData.uid
                }
            }
        }

        override fun isYandexoid(): Boolean {
            return AccountType.fromStringType(loginData.type) == AccountType.TEAM
        }

        override fun isAuthorized(): Boolean {
            return !TextUtils.isEmpty(loginData.token)
        }

        override fun isMailish(): Boolean {
            return AccountType.fromStringType(loginData.type) == AccountType.MAILISH
        }

        override fun getStash(): PassportStash {
            return PassportStash { cell: String -> extraDataMap[loginData]?.get(cell) }
        }

        override fun getSocialProviderCode(): String? {
            return null // TODO authstub support MailProviders in LoginData
        }

        override fun getAndroidAccount(): Account {
            return loginData.toAccount()
        }

        override fun isPdd(): Boolean {
            return false
        }
    }

    companion object {
        const val AUTH_URL = "https://yandex.ru"

        const val AUTH_ERROR_ACCOUNT_NAME = "account_auth_url_problems"

        const val RELOGIN_ACTION = "mock_passport_api_relogin"
    }
}
