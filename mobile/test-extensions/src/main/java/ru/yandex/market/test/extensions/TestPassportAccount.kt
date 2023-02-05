package ru.yandex.market.test.extensions

import android.accounts.Account
import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportStash
import com.yandex.passport.api.PassportUid
import java.util.Date

class TestPassportAccount(
    uid: Long = 42L,
    private val firstName: String = "",
    private val lastName: String = "",
    private val publicId: String = "",
    private val isYandexoid: Boolean = false
) : PassportAccount {
    private val uid: PassportUid = PassportUid.Factory.from(Passport.PASSPORT_ENVIRONMENT_PRODUCTION, uid)

    override fun getUid() = uid
    override fun getNativeDefaultEmail(): String? = null
    override fun getStash(): PassportStash = PassportStash { null }
    override fun isMailish() = false
    override fun isAvatarEmpty() = false
    override fun getSocialProviderCode(): String? = null
    override fun getAndroidAccount() = Account("name", "type")
    override fun getPrimaryDisplayName() = "primaryName"
    override fun getSecondaryDisplayName() = "secondaryName"
    override fun getAvatarUrl(): String? = null
    override fun isYandexoid() = isYandexoid
    override fun isBetaTester() = false
    override fun isAuthorized() = false
    override fun getFirstName() = firstName
    override fun getPublicId(): String? = publicId
    override fun getLastName(): String = lastName
    override fun isLite() = false
    override fun isSocial() = false
    override fun isPhonish() = false
    override fun getBirthday(): Date? = null
    override fun isPdd() = true
    override fun hasPlus() = true
}