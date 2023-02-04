package ru.auto.ara.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportAccountNotAuthorizedProperties
import com.yandex.passport.api.PassportAccountUpgrader
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportAuthByQrProperties
import com.yandex.passport.api.PassportAuthorizationUrlProperties
import com.yandex.passport.api.PassportAutoLoginProperties
import com.yandex.passport.api.PassportAutoLoginResult
import com.yandex.passport.api.PassportBindPhoneProperties
import com.yandex.passport.api.PassportCode
import com.yandex.passport.api.PassportCookie
import com.yandex.passport.api.PassportCredentialProvider
import com.yandex.passport.api.PassportCredentials
import com.yandex.passport.api.PassportDeviceCode
import com.yandex.passport.api.PassportEnvironment
import com.yandex.passport.api.PassportFilter
import com.yandex.passport.api.PassportLoginProperties
import com.yandex.passport.api.PassportPaymentAuthArguments
import com.yandex.passport.api.PassportPersonProfile
import com.yandex.passport.api.PassportSocialApplicationBindProperties
import com.yandex.passport.api.PassportSocialBindProperties
import com.yandex.passport.api.PassportTheme
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportTrackId
import com.yandex.passport.api.PassportTurboAppAuthProperties
import com.yandex.passport.api.PassportTurboAppJwtToken
import com.yandex.passport.api.PassportUid
import com.yandex.passport.api.exception.PassportAccountNotFoundException
import com.yandex.passport.internal.ClientToken

private const val MOCK_TOKEN_VALUE = "11111"
private const val MOCK_TOKEN_CLIENT_ID = "111111111"
private const val MOCK_URL = "http://auto.ru"
private val mockClientToken = ClientToken(MOCK_TOKEN_VALUE, MOCK_TOKEN_CLIENT_ID)

@Suppress("NotImplementedDeclaration") // this is just mock so that ui-tests don't interact with real passport api
class TestPassportApi : PassportApi {
    override fun getAccount(uid: PassportUid): PassportAccount {
        throw PassportAccountNotFoundException(uid)
    }

    override fun getAccount(accountName: String): PassportAccount {
        throw PassportAccountNotFoundException(accountName)
    }

    override fun getCurrentAccount(): PassportAccount? = null

    override fun setCurrentAccount(uid: PassportUid) {
        // implement if needed
    }

    override fun getAccounts(filter: PassportFilter): List<PassportAccount> = emptyList()

    override fun getToken(uid: PassportUid): PassportToken = mockClientToken

    override fun getToken(uid: PassportUid, arguments: PassportPaymentAuthArguments): PassportToken = mockClientToken

    override fun getToken(uid: PassportUid, credentials: PassportCredentials): PassportToken = mockClientToken

    override fun getCode(uid: PassportUid): PassportCode {
        throw PassportAccountNotFoundException(uid)
    }

    override fun getCode(uid: PassportUid, credentials: PassportCredentials): PassportCode {
        throw PassportAccountNotFoundException(uid)
    }

    override fun getCode(uid: PassportUid, credentialProvider: PassportCredentialProvider): PassportCode {
        throw PassportAccountNotFoundException(uid)
    }

    override fun dropToken(token: String) {
        // implement if needed
    }

    override fun stashValue(uid: PassportUid, cell: String, value: String?) {
        // implement if needed
    }

    override fun stashValue(uids: MutableList<PassportUid>, cell: String, value: String?) {
        // implement if needed
    }

    override fun createLoginIntent(context: Context, loginProperties: PassportLoginProperties): Intent = Intent()

    override fun getAuthorizationUrl(properties: PassportAuthorizationUrlProperties): String = MOCK_URL

    override fun getAuthorizationUrl(uid: PassportUid, returnUrl: String, tld: String, yandexuidCookieValue: String?): String =
        MOCK_URL

    override fun authorizeByCode(code: PassportCode): PassportAccount {
        TODO("Not yet implemented")
    }

    override fun authorizeByCookie(cookie: PassportCookie): PassportAccount {
        TODO("Not yet implemented")
    }

    override fun tryAutoLogin(autoLoginProperties: PassportAutoLoginProperties): PassportAccount {
        TODO("Not yet implemented")
    }

    override fun tryAutoLogin(context: Context, properties: PassportAutoLoginProperties): PassportAutoLoginResult {
        TODO("Not yet implemented")
    }

    override fun createAutoLoginIntent(
        context: Context,
        uid: PassportUid,
        autoLoginProperties: PassportAutoLoginProperties,
    ): Intent = Intent()

    override fun logout(uid: PassportUid) {
        // implement if needed
    }

    override fun removeAccount(uid: PassportUid) {
        // implement if needed
    }

    override fun createSocialApplicationBindIntent(
        context: Context,
        properties: PassportSocialApplicationBindProperties,
    ): Intent = Intent()

    override fun createSocialBindIntent(context: Context, properties: PassportSocialBindProperties): Intent = Intent()

    override fun getLinkageCandidate(uid: PassportUid): PassportAccount? = null

    override fun performLinkageForce(firstUid: PassportUid, secondUid: PassportUid) {
        // implement if needed
    }

    override fun onPushMessageReceived(from: String, data: Bundle): Boolean = false

    override fun onInstanceIdTokenRefresh() {
        // implement if needed
    }

    override fun createBindPhoneIntent(context: Context, properties: PassportBindPhoneProperties): Intent = Intent()

    override fun updatePersonProfile(uid: PassportUid, personProfile: PassportPersonProfile) {
        // implement if needed
    }

    override fun getPersonProfile(uid: PassportUid, needDisplayNameVariants: Boolean): PassportPersonProfile {
        TODO("Not yet implemented")
    }

    override fun updateAvatar(uid: PassportUid, uri: Uri) {
        // implement if needed
    }

    override fun addAccount(passportEnvironment: PassportEnvironment, masterToken: String): PassportAccount {
        TODO("Not yet implemented")
    }

    override fun createAccountNotAuthorizedIntent(context: Context, properties: PassportAccountNotAuthorizedProperties): Intent =
        Intent()

    override fun getDeviceCode(environment: PassportEnvironment, deviceName: String?): PassportDeviceCode {
        TODO("Not yet implemented")
    }

    override fun getDeviceCode(environment: PassportEnvironment, deviceName: String?, clientBound: Boolean): PassportDeviceCode {
        TODO("Not yet implemented")
    }

    override fun acceptDeviceAuthorization(uid: PassportUid, userCode: String) {
        // implement if needed
    }

    override fun authorizeByDeviceCode(environment: PassportEnvironment, deviceCode: String): PassportAccount {
        TODO("Not yet implemented")
    }

    override fun performSync(uid: PassportUid) {
        // implement if needed
    }

    override fun sendAuthToTrack(uid: PassportUid, trackId: String) {
        // implement if needed
    }

    override fun createSendAuthToTrackIntent(
        context: Context,
        uri: String,
        passportUid: PassportUid?,
        useSecureDialogStyle: Boolean,
    ): Intent = Intent()

    override fun createAuthorizeByTrackIdIntent(
        context: Context,
        loginProperties: PassportLoginProperties,
        trackId: PassportTrackId,
    ): Intent = Intent()

    override fun getAccountManagementUrl(uid: PassportUid): Uri = Uri.EMPTY

    override fun acceptAuthInTrack(uid: PassportUid, url: Uri): Boolean = false

    override fun createAuthorizationByQrIntent(context: Context, environment: PassportEnvironment, theme: PassportTheme): Intent =
        Intent()

    override fun createAuthorizationByQrIntent(context: Context, properties: PassportAuthByQrProperties): Intent = Intent()

    override fun createTurboAppAuthIntent(context: Context, properties: PassportTurboAppAuthProperties): Intent = Intent()

    override fun getTurboAppAnonymizedUserInfo(properties: PassportTurboAppAuthProperties): PassportTurboAppJwtToken {
        TODO("Not yet implemented")
    }

    override fun getTurboAppUserInfo(environment: PassportEnvironment, oauthToken: String): PassportTurboAppJwtToken {
        TODO("Not yet implemented")
    }

    override fun getAccountUpgrader(): PassportAccountUpgrader {
        TODO("Not yet implemented")
    }

}
