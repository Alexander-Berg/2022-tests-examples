package com.yandex.mail.wrappers

import com.yandex.mail.network.response.MessageBodyJson

data class AttachmentWrapper(
    val name: String,
    val url: String,
    var hid: String,
    val size: Long,
    val previewSupported: Boolean,
    val mimeType: String,
    val attachmentClass: String,
    val disk: Boolean
) {

    fun generateMessageAttach() = MessageBodyJson.Attach(
        hid, name, size, url, disk, 0, previewSupported, false, attachmentClass, mimeType, null
    )

    class AttachmentWrapperBuilder internal constructor() {

        private var name: String? = null

        private var url: String? = null

        private var hid: String? = null

        private var size: Long? = null

        private var previewSupported: Boolean = false

        private var mimeType: String? = null

        private var attachmentClass: String? = null

        private var disk: Boolean = false

        fun name(name: String) = apply { this.name = name }

        fun url(url: String) = apply { this.url = url }

        fun hid(hid: String) = apply { this.hid = hid }

        fun size(size: Long) = apply { this.size = size }

        fun previewSupported(previewSupported: Boolean) = apply { this.previewSupported = previewSupported }

        fun mimeType(mimeType: String) = apply { this.mimeType = mimeType }

        fun attachmentClass(attachmentClass: String) = apply { this.attachmentClass = attachmentClass }

        fun disk(disk: Boolean) = apply { this.disk = disk }

        fun build(): AttachmentWrapper {
            return AttachmentWrapper(name!!, url!!, hid!!, size!!, previewSupported, mimeType!!, attachmentClass!!, disk)
        }
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun newTextAttachment(name: String, content: String, hid: String = "") = builder()
            .size(content.length.toLong())
            .hid(hid)
            .name(name)
            .url("")
            .previewSupported(true)
            .attachmentClass("")
            .mimeType("text/plain")
            .disk(false)
            .build()

        @JvmStatic
        fun newImageAttachment(name: String, hid: String): AttachmentWrapper = builder()
            .name(name)
            .url("url")
            .hid(hid)
            .size(1000)
            .previewSupported(true)
            .mimeType("image/jpeg")
            .attachmentClass("image")
            .disk(false)
            .build()

        @JvmStatic
        fun newBaseAttachmentBuilder(hid: String) = builder()
            .name("attach_$hid")
            .url("url")
            .hid(hid)
            .size(1000)
            .previewSupported(true)
            .mimeType("unknown")
            .attachmentClass("image")
            .disk(false)

        @JvmStatic
        fun builder(): AttachmentWrapperBuilder {
            return AttachmentWrapperBuilder()
        }
    }
}
