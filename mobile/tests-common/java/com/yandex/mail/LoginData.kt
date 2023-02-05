package com.yandex.mail

import android.accounts.Account
import java.util.concurrent.atomic.AtomicLong

data class LoginData @JvmOverloads constructor(
    /**
     * Name should be unique among the accounts used in the test.
     */
    @JvmField val name: String,
    @JvmField val type: String,
    @JvmField val token: String? = null,
    @JvmField val uid: Long = UID_GENERATOR.incrementAndGet() // TODO add shift to be sure that its not used as Int?
) { // todo: check nullability

    fun getName(): String { // todo: fix/remove
        return name
    }

    fun toAccount(): Account {
        return Account(name, SYSTEM_ACCOUNT_TYPE)
    }

    companion object {
        const val SYSTEM_ACCOUNT_TYPE = "com.yandex.passport"

        val UID_GENERATOR = AtomicLong(Int.MAX_VALUE.toLong() + 1) // to avoid usage as Int
    }
}
