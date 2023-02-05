package com.yandex.mail.wrappers

import com.google.gson.Gson
import com.yandex.mail.entity.MessageBodyMeta.Companion.DEFAULT_CONTENT_TYPE
import com.yandex.mail.network.json.response.Recipient
import com.yandex.mail.network.response.MessageAttachmentsMeta
import com.yandex.mail.network.response.MessageBodyJson
import com.yandex.mail.network.response.MessageMetaJson
import com.yandex.mail.storage.MessageStatus
import com.yandex.mail.tools.MockServerTools.createOkStatus
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.util.Date

data class MessageWrapper(
    val mid: String,
    var folder: FolderWrapper,
    val tid: String?,
    val labels: MutableList<LabelWrapper>,
    var read: Boolean = false,
    val timestamp: Date,
    val subjPrefix: String,
    val subjText: String,
    var from: Recipient,
    val types: MutableList<MessageStatus.Type>,
    val recipients: MutableList<Recipient>,
    var attachments: MutableList<AttachmentWrapper>,
    /**
     * If content is null, the message does not have a body. We return TMP_ERROR for message body request in this case.
     */
    var content: String? = null,
    val contentType: String,
    val rfcId: String,
    val references: List<String>
) { // todo: check nullability


    fun generateMessageMeta(): MessageMetaJson {
        var fullSize = 0L
        var hasDiskAttaches = false
        val jsonAttachments: MutableList<MessageBodyJson.Attach> = ArrayList()
        for (attachment in attachments) {
            fullSize += attachment.size
            if (attachment.disk) {
                hasDiskAttaches = true
            }
            jsonAttachments.add(attachment.generateMessageAttach())
        }
        val json = MessageMetaJson(
            java.lang.Long.valueOf(mid),
            java.lang.Long.valueOf(folder.serverFid),
            labels.map { it.serverLid },
            listOf(if (read) MessageStatus.Status.READ.id else MessageStatus.Status.UNREAD.id),
            timestamp.time / 1000,
            !attachments.isEmpty(),
            false,
            types.map { it.id }.toIntArray(),
            subjPrefix,
            subjText,
            com.yandex.mail.network.response.RecipientJson(
                from.email!!,
                from.name,
                com.yandex.mail.network.response.RecipientJson.Type.fromId(from.type!!.id)
            ),
            content!!,
            MessageAttachmentsMeta(
                attachments.size,
                fullSize,
                hasDiskAttaches,
                jsonAttachments
            ),
            null,
            tid?.let { java.lang.Long.valueOf(it) }
        )

        return json
    }

    fun generateMessageBodyJson(): MessageBodyJson {
        val bodyJson = MessageBodyJson()

        when (content) {
            null -> return bodyJson.apply { status = createOkStatus() }
            "IOException" -> throw IOException("Ooooops, something goes wronng")
        }

        val info = MessageBodyJson.Info(
            java.lang.Long.valueOf(mid),
            attachments.map { it.generateMessageAttach() },
            recipients.map { r ->
                com.yandex.mail.network.response.RecipientJson(
                    r.email!!,
                    r.name,
                    com.yandex.mail.network.response.RecipientJson.Type.fromId(r.type!!.id)
                )
            },
            rfcId,
            StringUtils.join(references, " ")
        )
        bodyJson.info = info

        val body = MessageBodyJson.Body(
            "1", content, "text/plain", "lang", "originalLang"
        )
        bodyJson.body = listOf(body)

        bodyJson.status = createOkStatus()

        return bodyJson
    }

    val isUnread: Boolean
        get() = !read

    class MessageWrapperBuilder {
        var mid: String? = null
        var folder: FolderWrapper? = null
        var tid: String? = null
        var labels: MutableList<LabelWrapper> = mutableListOf()
        var read: Boolean = false
        var timestamp: Date? = null
        var subjPrefix: String? = null
        var subjText: String? = null
        var from: Recipient? = null
        var types: MutableList<MessageStatus.Type> = mutableListOf()
        var recipients: MutableList<Recipient> = mutableListOf()
        var attachments: MutableList<AttachmentWrapper> = mutableListOf()
        /**
         * If content is null, the message does not have a body. We return TMP_ERROR for message body request in this case.
         */
        var content: String? = null
        var contentType: String = DEFAULT_CONTENT_TYPE
        var rfcId: String? = null
        var references: MutableList<String> = mutableListOf()

        fun attachment(attachment: AttachmentWrapper): MessageWrapperBuilder {
            this.attachments.add(attachment)
            return this
        }

        fun attachments(attachments: List<AttachmentWrapper>): MessageWrapperBuilder {
            this.attachments.addAll(attachments)
            return this
        }

        fun bcc(emails: List<String>): MessageWrapperBuilder {
            this.recipients.addAll(emails.map { email -> Recipient(email, null, Recipient.Type.BCC) })
            return this
        }

        fun bcc(emails: Collection<String>): MessageWrapperBuilder {
            return bcc(emails.toList())
        }

        fun bcc(email: String): MessageWrapperBuilder {
            return bcc(listOf(email))
        }

        fun cc(emails: List<String>): MessageWrapperBuilder {
            this.recipients.addAll(emails.map { email -> Recipient(email, null, Recipient.Type.CC) })
            return this
        }

        fun cc(emails: Collection<String>): MessageWrapperBuilder {
            return cc(emails.toList())
        }

        fun cc(email: String): MessageWrapperBuilder {
            return cc(listOf(email))
        }

        fun content(content: String?): MessageWrapperBuilder {
            this.content = content
            return this
        }

        fun contentType(contentType: String): MessageWrapperBuilder {
            this.contentType = contentType
            return this
        }

        fun folder(folder: FolderWrapper): MessageWrapperBuilder {
            this.folder = folder
            return this
        }

        fun read(read: Boolean): MessageWrapperBuilder {
            this.read = read
            return this
        }

        fun from(email: String): MessageWrapperBuilder {
            this.from = Recipient(email, null, Recipient.Type.FROM)
            return this
        }

        fun from(email: String, name: String): MessageWrapperBuilder {
            this.from = Recipient(email, name, Recipient.Type.FROM)
            return this
        }

        fun label(label: LabelWrapper): MessageWrapperBuilder {
            return labels(listOf(label))
        }

        fun labels(vararg labels: LabelWrapper): MessageWrapperBuilder {
            return labels(labels.toList())
        }

        fun labels(labels: Collection<LabelWrapper>): MessageWrapperBuilder {
            this.labels.addAll(labels)
            return this
        }

        fun mid(mid: String): MessageWrapperBuilder {
            this.mid = mid
            return this
        }

        fun references(vararg references: String): MessageWrapperBuilder {
            return references(references.toList())
        }

        fun references(references: List<String>): MessageWrapperBuilder {
            this.references.addAll(references)
            return this
        }

        fun rfcId(rfcId: String): MessageWrapperBuilder {
            this.rfcId = rfcId
            return this
        }

        fun subjPrefix(prefix: String): MessageWrapperBuilder {
            this.subjPrefix = prefix
            return this
        }

        fun subjText(subject: String): MessageWrapperBuilder {
            this.subjText = subject
            return this
        }

        fun tid(tid: String?): MessageWrapperBuilder {
            this.tid = tid
            return this
        }

        fun timestamp(time: Date): MessageWrapperBuilder {
            this.timestamp = time
            return this
        }

        fun to(emails: List<String>): MessageWrapperBuilder {
            this.recipients.addAll(emails.map { email -> Recipient(email, null, Recipient.Type.TO) })
            return this
        }

        fun to(emails: Collection<String>): MessageWrapperBuilder {
            return to(emails.toList())
        }

        fun to(email: String): MessageWrapperBuilder {
            return to(listOf(email))
        }

        fun types(vararg types: MessageStatus.Type): MessageWrapperBuilder {
            return types(types.toList())
        }

        fun types(types: List<MessageStatus.Type>): MessageWrapperBuilder {
            this.types.addAll(types)
            return this
        }

        fun build(): MessageWrapper {
            return MessageWrapper(
                mid!!,
                folder!!,
                tid,
                labels,
                read,
                timestamp!!,
                subjPrefix!!,
                subjText!!,
                from!!,
                types,
                recipients,
                attachments,
                content,
                contentType,
                rfcId!!,
                references
            )
        }
    }

    companion object {

        @JvmStatic fun generateMessageContentsResponse(gson: Gson, messages: Collection<MessageWrapper>): String {
            val contents = messages.map { it.generateMessageBodyJson() }.toTypedArray()
            return gson.toJson(contents)
        }
    }
}
