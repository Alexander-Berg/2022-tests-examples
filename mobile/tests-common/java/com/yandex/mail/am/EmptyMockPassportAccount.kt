package com.yandex.mail.am

import android.accounts.Account
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportStash
import com.yandex.passport.api.PassportUid
import java.util.Date

object EmptyMockPassportAccount : PassportAccount {
    override fun getUid(): PassportUid {
        throw NotImplementedError()
    }

    override fun getPrimaryDisplayName(): String {
        throw NotImplementedError()
    }

    override fun getSecondaryDisplayName(): String? {
        throw NotImplementedError()
    }

    override fun getAvatarUrl(): String? {
        throw NotImplementedError()
    }

    override fun isAvatarEmpty(): Boolean {
        throw NotImplementedError()
    }

    override fun getNativeDefaultEmail(): String? {
        throw NotImplementedError()
    }

    override fun isYandexoid(): Boolean {
        throw NotImplementedError()
    }

    override fun isBetaTester(): Boolean {
        throw NotImplementedError()
    }

    override fun isAuthorized(): Boolean {
        throw NotImplementedError()
    }

    override fun getStash(): PassportStash {
        throw NotImplementedError()
    }

    override fun getAndroidAccount(): Account {
        throw NotImplementedError()
    }

    override fun isMailish(): Boolean {
        throw NotImplementedError()
    }

    override fun getSocialProviderCode(): String? {
        throw NotImplementedError()
    }

    override fun hasPlus(): Boolean {
        throw NotImplementedError()
    }

    override fun isPhonish(): Boolean {
        throw NotImplementedError()
    }

    override fun isSocial(): Boolean {
        throw NotImplementedError()
    }

    override fun isPdd(): Boolean {
        throw NotImplementedError()
    }

    override fun isLite(): Boolean {
        throw NotImplementedError()
    }

    override fun getFirstName(): String? {
        throw NotImplementedError()
    }

    override fun getLastName(): String? {
        throw NotImplementedError()
    }

    override fun getBirthday(): Date? {
        throw NotImplementedError()
    }

    override fun getPublicId(): String? {
        TODO("Not yet implemented")
    }
}
