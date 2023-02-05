package ru.yandex.yandexbus.inhouse.service.auth

import android.accounts.Account
import android.net.Uri
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportUid
import java.util.Date

fun stubUserData(
    uid: Uid = Uid(1),
    token: Token = Token.Valid("token"),
    displayName: String = "testName"
): Pair<PassportAccount, User.Authorized> {
    val account = StubPassportAccount(uid.toPassportUid(), token, displayName)
    val user = account.toUser()
    return account to user
}

fun StubPassportAccount.toUser(): User.Authorized = User.Authorized(
    uid.toUid(),
    token,
    primaryDisplayName,
    nativeDefaultEmail,
    if (avatarUrl != null) Uri.parse(avatarUrl) else null,
    hasPlus()
)

data class StubPassportAccount(
    private val uid: PassportUid,
    val token: Token,
    private val primaryDisplayName: String
) : PassportAccount {

    override fun getUid() = uid

    override fun getNativeDefaultEmail(): String? = null

    override fun getStash() = throw NotImplementedError()

    override fun isMailish() = false

    override fun isPhonish() = false

    override fun isLite() = false

    override fun isSocial() = false

    override fun isPdd() = false

    override fun hasPlus() = false

    override fun getSocialProviderCode(): String? = null

    override fun getAndroidAccount() = Account(primaryDisplayName, "test")

    override fun getPrimaryDisplayName(): String = primaryDisplayName

    override fun getSecondaryDisplayName(): String? = null

    override fun isAvatarEmpty() = true

    override fun getAvatarUrl(): String? = null

    override fun isYandexoid() = false

    override fun isBetaTester() = false

    override fun isAuthorized() = token is Token.Valid

    override fun getFirstName(): String? = null

    override fun getBirthday(): Date? = null

    override fun getLastName(): String? = null
}