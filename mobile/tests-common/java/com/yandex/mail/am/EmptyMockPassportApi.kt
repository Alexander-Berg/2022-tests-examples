package com.yandex.mail.am

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportAccountNotAuthorizedProperties
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportAuthByQrProperties
import com.yandex.passport.api.PassportAuthorizationUrlProperties
import com.yandex.passport.api.PassportAutoLoginProperties
import com.yandex.passport.api.PassportAutoLoginResult
import com.yandex.passport.api.PassportBindPhoneProperties
import com.yandex.passport.api.PassportCode
import com.yandex.passport.api.PassportCookie
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

class EmptyMockPassportApi : PassportApi {

    override fun authorizeByCode(code: PassportCode): PassportAccount {
        throw NotImplementedError()
    }

    override fun removeAccount(uid: PassportUid) {
        throw NotImplementedError()
    }

    override fun getTurboAppAnonymizedUserInfo(properties: PassportTurboAppAuthProperties): PassportTurboAppJwtToken {
        throw NotImplementedError()
    }

    override fun createSendAuthToTrackIntent(context: Context, uri: String, passportUid: PassportUid?, useSecureDialogStyle: Boolean): Intent {
        throw NotImplementedError()
    }

    override fun updateAvatar(uid: PassportUid, uri: Uri) {
        throw NotImplementedError()
    }

    override fun getCurrentAccount(): PassportAccount? {
        throw NotImplementedError()
    }

    override fun createAuthorizationByQrIntent(context: Context, environment: PassportEnvironment, theme: PassportTheme): Intent {
        throw NotImplementedError()
    }

    override fun createAuthorizationByQrIntent(context: Context, properties: PassportAuthByQrProperties): Intent {
        throw NotImplementedError()
    }

    override fun createSocialApplicationBindIntent(context: Context, properties: PassportSocialApplicationBindProperties): Intent {
        throw NotImplementedError()
    }

    override fun getToken(uid: PassportUid): PassportToken {
        throw NotImplementedError()
    }

    override fun getToken(uid: PassportUid, arguments: PassportPaymentAuthArguments): PassportToken {
        throw NotImplementedError()
    }

    override fun getToken(uid: PassportUid, credentials: PassportCredentials): PassportToken {
        throw NotImplementedError()
    }

    override fun performLinkageForce(firstUid: PassportUid, secondUid: PassportUid) {
        throw NotImplementedError()
    }

    override fun authorizeByCookie(cookie: PassportCookie): PassportAccount {
        throw NotImplementedError()
    }

    override fun getDeviceCode(environment: PassportEnvironment, deviceName: String?): PassportDeviceCode {
        throw NotImplementedError()
    }

    override fun getDeviceCode(environment: PassportEnvironment, deviceName: String?, clientBound: Boolean): PassportDeviceCode {
        throw NotImplementedError()
    }

    override fun createAutoLoginIntent(context: Context, uid: PassportUid, autoLoginProperties: PassportAutoLoginProperties): Intent {
        throw NotImplementedError()
    }

    override fun createAccountNotAuthorizedIntent(context: Context, properties: PassportAccountNotAuthorizedProperties): Intent {
        throw NotImplementedError()
    }

    override fun getAccountManagementUrl(uid: PassportUid): Uri {
        throw NotImplementedError()
    }

    override fun createSocialBindIntent(context: Context, properties: PassportSocialBindProperties): Intent {
        throw NotImplementedError()
    }

    override fun logout(uid: PassportUid) {
        throw NotImplementedError()
    }

    override fun dropToken(token: String) {
        throw NotImplementedError()
    }

    override fun getAccount(uid: PassportUid): PassportAccount {
        throw NotImplementedError()
    }

    override fun getAccount(accountName: String): PassportAccount {
        throw NotImplementedError()
    }

    override fun createAuthorizeByTrackIdIntent(context: Context, loginProperties: PassportLoginProperties, trackId: PassportTrackId): Intent {
        throw NotImplementedError()
    }

    override fun onPushMessageReceived(from: String, data: Bundle): Boolean {
        throw NotImplementedError()
    }

    override fun getAccounts(filter: PassportFilter): MutableList<PassportAccount> {
        throw NotImplementedError()
    }

    override fun getPersonProfile(uid: PassportUid, needDisplayNameVariants: Boolean): PassportPersonProfile {
        throw NotImplementedError()
    }

    override fun onInstanceIdTokenRefresh() {
        throw NotImplementedError()
    }

    override fun sendAuthToTrack(uid: PassportUid, trackId: String) {
        throw NotImplementedError()
    }

    override fun getTurboAppUserInfo(environment: PassportEnvironment, oauthToken: String): PassportTurboAppJwtToken {
        throw NotImplementedError()
    }

    override fun getAuthorizationUrl(properties: PassportAuthorizationUrlProperties): String {
        throw NotImplementedError()
    }

    override fun getAuthorizationUrl(uid: PassportUid, returnUrl: String, tld: String, yandexuidCookieValue: String?): String {
        throw NotImplementedError()
    }

    override fun updatePersonProfile(uid: PassportUid, personProfile: PassportPersonProfile) {
        throw NotImplementedError()
    }

    override fun setCurrentAccount(uid: PassportUid) {
        throw NotImplementedError()
    }

    override fun createBindPhoneIntent(context: Context, properties: PassportBindPhoneProperties): Intent {
        throw NotImplementedError()
    }

    override fun authorizeByDeviceCode(environment: PassportEnvironment, deviceCode: String): PassportAccount {
        throw NotImplementedError()
    }

    override fun stashValue(uid: PassportUid, cell: String, value: String?) {
        throw NotImplementedError()
    }

    override fun stashValue(uids: MutableList<PassportUid>, cell: String, value: String?) {
        throw NotImplementedError()
    }

    override fun acceptDeviceAuthorization(uid: PassportUid, userCode: String) {
        throw NotImplementedError()
    }

    override fun acceptAuthInTrack(uid: PassportUid, url: Uri): Boolean {
        throw NotImplementedError()
    }

    override fun createLoginIntent(context: Context, loginProperties: PassportLoginProperties): Intent {
        throw NotImplementedError()
    }

    override fun getLinkageCandidate(uid: PassportUid): PassportAccount? {
        throw NotImplementedError()
    }

    override fun getCode(uid: PassportUid): PassportCode {
        throw NotImplementedError()
    }

    override fun getCode(uid: PassportUid, credentials: PassportCredentials): PassportCode {
        throw NotImplementedError()
    }

    override fun tryAutoLogin(autoLoginProperties: PassportAutoLoginProperties): PassportAccount {
        throw NotImplementedError()
    }

    override fun tryAutoLogin(context: Context, properties: PassportAutoLoginProperties): PassportAutoLoginResult {
        throw NotImplementedError()
    }

    override fun performSync(uid: PassportUid) {
        throw NotImplementedError()
    }

    override fun createTurboAppAuthIntent(context: Context, properties: PassportTurboAppAuthProperties): Intent {
        throw NotImplementedError()
    }

    override fun addAccount(passportEnvironment: PassportEnvironment, masterToken: String): PassportAccount {
        throw NotImplementedError()
    }
}
