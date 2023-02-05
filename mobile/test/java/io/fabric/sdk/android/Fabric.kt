package io.fabric.sdk.android

import android.content.Context

class Fabric {

    companion object {
        var initializationChecked = false
        var initialized = false

        fun clear() {
            initializationChecked = false
            initialized = false
        }

        @JvmStatic
        fun isInitialized(): Boolean {
            initializationChecked = true
            return false
        }

        @JvmStatic
        fun with(context: Context, vararg kits: Kit<*>): Fabric {
            initialized = true
            return Fabric()
        }
    }
}
