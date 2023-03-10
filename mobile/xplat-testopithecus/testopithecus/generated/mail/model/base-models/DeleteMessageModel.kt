// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/base-models/delete-message-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class DeleteMessageModel(private var model: MessageListDisplayModel, private var accHandler: MailAppModelHandler): DeleteMessage {
    private var lastDeleteMessageTime: Long? = null
    private var deletedMessageIdToFolder: YSMap<MessageId, FolderName> = mutableMapOf<MessageId, FolderName>()
    open fun resetLastDeleteMessageTime(): Unit {
        this.lastDeleteMessageTime = null
    }

    open fun getLastDeleteMessageTime(): Long? {
        return this.lastDeleteMessageTime
    }

    open fun getDeletedMessageIdToFolder(): YSMap<MessageId, FolderName> {
        return this.deletedMessageIdToFolder
    }

    open override fun deleteMessage(order: Int): Unit {
        this.deleteMessages(YSSet<Int>(mutableListOf(order)))
    }

    open fun deleteOpenedMessage(mid: MessageId): Unit {
        this.deletedMessageIdToFolder.clear()
        this.deleteMessageByMid(mid)
        this.lastDeleteMessageTime = currentTimeMs()
    }

    private fun deleteMessageByMid(mid: MessageId): Unit {
        val folderName = this.accHandler.getCurrentAccount().messagesDB.storedFolder(mid)
        this.deletedMessageIdToFolder.set(mid, folderName)
        this.accHandler.getCurrentAccount().messagesDB.removeMessage(mid)
    }

    open fun deleteMessages(orders: YSSet<Int>): Unit {
        this.deletedMessageIdToFolder.clear()
        for (mid in this.model.getMidsByOrders(orders)) {
            this.deleteMessageByMid(mid)
        }
        this.lastDeleteMessageTime = currentTimeMs()
    }

}

