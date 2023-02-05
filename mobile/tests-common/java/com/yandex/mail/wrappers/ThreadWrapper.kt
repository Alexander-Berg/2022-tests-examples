package com.yandex.mail.wrappers

import android.os.SystemClock
import com.yandex.mail.fakeserver.AccountWrapper.Companion.MESSAGE_ORDER_COMPARATOR
import com.yandex.mail.network.response.MessageAttachmentsMeta
import com.yandex.mail.network.response.MessageBodyJson
import com.yandex.mail.network.response.RecipientJson
import com.yandex.mail.network.response.ThreadMeta
import com.yandex.mail.storage.MessageStatus
import java.util.Collections.sort

data class ThreadWrapper(val tid: String, val messages: List<MessageWrapper>) {

    init {
        sort(messages, MESSAGE_ORDER_COMPARATOR)
    }

    // if timestamp is null then we don't care about it - it's ok to return 0
    val timestamp: Long
        get() = messages.firstOrNull()?.timestamp?.time ?: 0L

    fun generateThreadMeta(folderId: Long): ThreadMeta {
        val top = messages.first { it.folder.serverFid == folderId.toString() }
        val topMessage = top.generateMessageMeta()

        var fullSize = 0L
        var hasDiskAttaches = false
        val jsonAttachments: MutableList<MessageBodyJson.Attach> = ArrayList()
        for (attachment in top.attachments) {
            fullSize += attachment.size
            if (attachment.disk) {
                hasDiskAttaches = true
            }
            jsonAttachments.add(attachment.generateMessageAttach())
        }
        return ThreadMeta(
            mid = java.lang.Long.valueOf(top.mid),
            fid = java.lang.Long.valueOf(top.folder.serverFid),
            tid = java.lang.Long.valueOf(this@ThreadWrapper.tid),
            lid = top.labels.map { it.serverLid },
            status = listOf(if (top.read) MessageStatus.Status.READ.id else MessageStatus.Status.UNREAD.id),
            utc_timestamp = top.timestamp.time / 1000,
            hasAttach = !top.attachments.isEmpty(),
            subjPrefix = messages.firstOrNull { it.subjPrefix.isNotEmpty() }?.subjPrefix ?: "",
            subjText = top.subjText,
            from = with(top.from) { RecipientJson(email!!, name, RecipientJson.Type.fromId(type!!.id)) },
            firstLine = topMessage.firstLine,
            types = topMessage.types,
            scn = SystemClock.elapsedRealtime(),
            attachments = MessageAttachmentsMeta(
                top.attachments.size,
                fullSize,
                hasDiskAttaches,
                jsonAttachments
            ),
            recipients = emptyList(),
            subjEmpty = false,
            _threadCount = messages.size
        )
    }

    class ThreadWrapperBuilder internal constructor() {
        private var tid: String? = null
        private var messages: List<MessageWrapper>? = null

        fun tid(tid: String) = apply { this.tid = tid }
        fun messages(messages: List<MessageWrapper>) = apply { this.messages = messages }
        fun build() = ThreadWrapper(tid!!, messages!!)
    }

    companion object {
        @JvmStatic
        fun builder(): ThreadWrapperBuilder {
            return ThreadWrapperBuilder()
        }
    }
}
