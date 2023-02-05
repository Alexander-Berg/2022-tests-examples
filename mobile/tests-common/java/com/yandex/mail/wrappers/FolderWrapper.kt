package com.yandex.mail.wrappers

import com.yandex.mail.entity.FolderType

data class FolderWrapper(
    val serverFid: String,
    var parent: String, // todo: make immutable?
    var name: String, // todo: make immutable?
    /**
     * Type from [FolderType]
     */
    val type: Int,
    val position: Int
) {

    class FolderWrapperBuilder internal constructor() {

        private var serverFid: String? = null

        private var parent: String? = null

        private var name: String? = null

        private var type: Int? = null

        private var position: Int = 0 // todo: don't use default?

        fun serverFid(serverFid: String) = apply { this.serverFid = serverFid }

        fun parent(parent: String) = apply { this.parent = parent }

        fun name(name: String) = apply { this.name = name }

        fun type(type: FolderType) = apply { this.type = type.serverType }

        fun position(position: Int) = apply { this.position = position }

        fun build(): FolderWrapper {
            return FolderWrapper(serverFid!!, parent!!, name!!, type!!, position)
        }
    }

    companion object {

        @JvmStatic fun builder(): FolderWrapperBuilder {
            return FolderWrapperBuilder()
        }
    }
}
