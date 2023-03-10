// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/mail-view-features.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class MessageViewerAndroidFeature private constructor(): Feature<MessageViewerAndroid>("MessageViewerAndroid", "Специфичные для андроида действия с открытым письмом.") {
    companion object {
        @JvmStatic var `get`: MessageViewerAndroidFeature = MessageViewerAndroidFeature()
    }
}

public interface MessageViewerAndroid {
    fun deleteMessageByIcon(): Unit
    fun getDefaultSourceLanguage(): LanguageName
}

public open class MessageViewerFeature private constructor(): Feature<MessageViewer>("MessageViewer", "Фича для управления открытым письмом. Несколько меток добавляются установкой нескольких чекбоксов в popup") {
    companion object {
        @JvmStatic var `get`: MessageViewerFeature = MessageViewerFeature()
    }
}

public interface MessageViewer {
    fun openMessage(order: Int): Unit
    fun isMessageOpened(): Boolean
    fun closeMessage(): Unit
    fun getOpenedMessage(): FullMessageView
    fun checkIfRead(): Boolean
    fun checkIfSpam(): Boolean
    fun checkIfImportant(): Boolean
    fun getLabels(): YSSet<String>
    fun deleteLabelsFromHeader(labels: YSArray<LabelName>): Unit
    fun markAsUnimportantFromHeader(): Unit
    fun arrowDownClick(): Unit
    fun arrowUpClick(): Unit
}

public open class ThreadViewNavigatorFeature private constructor(): Feature<ThreadViewNavigator>("ThreadViewNavigator", "Навигационный тулбар в просмотре письма, можно переключаться между письмами треда и " + "удалить/архивировать (в зависимости от действия по свайпу) тред. Есть во всех Android и в планшетах на IOS") {
    companion object {
        @JvmStatic val `get`: ThreadViewNavigatorFeature = ThreadViewNavigatorFeature()
    }
}

public interface ThreadViewNavigator {
    fun deleteCurrentThread(): Unit
    fun archiveCurrentThread(): Unit
}

public interface FullMessageView {
    val head: MessageView
    val to: YSSet<String>
    val body: String
    val lang: LanguageName
    val quickReply: Boolean
    val smartReplies: YSArray<String>
    fun tostring(): String
}

public interface MessageView {
    var from: String
    val to: String
    val subject: String
    val read: Boolean
    val important: Boolean
    var threadCounter: Int?
    val attachments: YSArray<AttachmentView>
    val firstLine: String
    val timestamp: Long
    fun tostring(): String
}

public interface AttachmentView {
    val displayName: String
}

