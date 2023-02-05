package com.yandex.metrica.profile

import com.yandex.metrica.impl.ob.qj

class BooleanAttribute(private val key: String) {
    fun withValue(value: Boolean): UserProfileUpdate<out qj> {
        UserProfile.boolParams[key] = value
        return UserProfileUpdate()
    }
}
