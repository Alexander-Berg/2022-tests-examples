package com.yandex.metrica.profile

import com.yandex.metrica.impl.ob.qj

class UserProfile {

    companion object {
        val params = mutableMapOf<String, String?>()
        val boolParams = mutableMapOf<String, Boolean?>()

        fun clear() {
            params.clear()
            boolParams.clear()
        }

        @JvmStatic
        fun newBuilder(): Builder {
            return Builder()
        }
    }

    class Builder internal constructor() {
        fun apply(userProfileUpdate: UserProfileUpdate<out qj>): Builder {
            return this
        }

        fun build(): UserProfile {
            return UserProfile()
        }
    }
}
