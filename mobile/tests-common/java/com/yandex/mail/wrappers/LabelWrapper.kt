package com.yandex.mail.wrappers

data class LabelWrapper(
    val serverLid: String,
    val displayName: String,
    val type: Int
) {

    class LabelWrapperBuilder internal constructor() {

        private var serverLid: String? = null

        private var displayName: String? = null

        private var type: Int? = null

        fun serverLid(serverLid: String) = apply { this.serverLid = serverLid }

        fun displayName(displayName: String) = apply { this.displayName = displayName }

        fun type(type: Int) = apply { this.type = type }

        fun build(): LabelWrapper {
            return LabelWrapper(serverLid!!, displayName!!, type!!)
        }
    }

    companion object {

        @JvmStatic fun builder(): LabelWrapperBuilder {
            return LabelWrapperBuilder()
        }
    }
}
