package com.yandex.metrica.profile

import com.yandex.metrica.impl.ob.qj

class StringAttribute(private val key: String) {
    fun withValue(value: String): UserProfileUpdate<out qj> {
        UserProfile.params[key] = value
        return UserProfileUpdate()
    }
}
