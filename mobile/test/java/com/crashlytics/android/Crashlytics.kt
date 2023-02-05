package com.crashlytics.android

import io.fabric.sdk.android.Kit

class Crashlytics : Kit<Any>() {

    companion object {
        var identifier: String? = null
        val params = mutableMapOf<String, String>()
        val boolParams = mutableMapOf<String, Boolean>()

        fun clear() {
            identifier = null
            params.clear()
            boolParams.clear()
        }

        @JvmStatic
        fun setUserIdentifier(identifier: String) {
            Crashlytics.identifier = identifier
        }

        @JvmStatic
        fun setString(key: String, value: String) {
            params[key] = value
        }

        @JvmStatic
        fun setBool(key: String, value: Boolean) {
            boolParams[key] = value
        }
    }
}
