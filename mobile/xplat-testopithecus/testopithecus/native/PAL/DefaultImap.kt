package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.testopithecus.common.*
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import kotlin.collections.map

class DefaultImap(account: MailAccountSpec) : Imap {
    private val connection: DefaultImapConnection = DefaultImapConnection(account)

    override fun connect(cb: (YSError?) -> Unit) {
        connection.getStore()
        cb(null)
    }

    override fun createFolder(folder: String, cb: (YSError?) -> Unit) {
        try {
            val root: Folder = connection.getStore().defaultFolder
            if (root.isOpen) {
                root.close()
            }
            val newFolder = root.getFolder(folder)
            if (!newFolder.exists()) {
                newFolder.create(Folder.HOLDS_MESSAGES)
            }
            cb(null)
        } catch (e: MessagingException) {
            cb(buildFailure(e))
        }
    }

    override fun openFolder(folder: String): XPromise<ImapFolderInfo> {
        return promise { resolve, reject ->
            try {
                val imapFolder = connection.getStore().getFolder(folder)
                if (!imapFolder.isOpen) {
                    imapFolder.open(Folder.READ_WRITE)
                }
                resolve(ImapFolderInfo(imapFolder.messageCount))
            } catch (e: MessagingException) {
                reject(buildFailure(e))
            }
        }
    }

    override fun disconnect(cb: (YSError?) -> Unit) {
        connection.getStore().close()
        cb(null)
    }

    override fun fetchAllFolders(): XPromise<YSArray<ImapFolderDisplay>> {
        return promise { resolve, reject ->
            try {
                val root: Folder = connection.getStore().defaultFolder
                val returnFolders: YSArray<ImapFolderDisplay> = arrayListOf()
                for (folder in root.list()) {
                    returnFolders.add(ImapFolderDisplay(folder.fullName))
                }
                resolve(returnFolders)
            } catch (e: MessagingException) {
                reject(buildFailure(e))
            }
        }
    }

    override fun deleteMessages(
        folder: String,
        messageCount: Int,
        cb: (YSError?) -> Unit
    ) {
        if (messageCount == 0) {
            return cb(null)
        }
        try {
            val currentFolder = connection.getStore().getFolder(folder)
            if (!currentFolder.isOpen) {
                currentFolder.open(Folder.READ_WRITE)
            }
            val folderMessages = currentFolder.messages
            for (message in folderMessages) {
                message.setFlag(Flags.Flag.DELETED, true)
            }
            cb(null)
        } catch (e: MessagingException) {
            cb(buildFailure(e))
        }
    }

    override fun expungeFolder(folder: String, cb: (YSError?) -> Unit) {
        try {
            val currentFolder = connection.getStore().getFolder(folder)
            if (!currentFolder.isOpen) {
                currentFolder.open(Folder.READ_WRITE)
            }
            currentFolder.expunge()
            currentFolder.close()
            cb(null)
        } catch (e: MessagingException) {
            cb(buildFailure(e))
        }
    }

    override fun deleteFolder(folder: String, cb: (YSError?) -> Unit) {
        try {
            val currentFolder = connection.getStore().getFolder(folder)
            if (currentFolder.isOpen) {
                currentFolder.close()
            }
            currentFolder.delete(true)
            cb(null)
        } catch (e: MessagingException) {
            cb(buildFailure(e))
        }
    }

    override fun appendMessage(
        folder: String,
        message: MessageSpec,
        cb: (YSError?) -> Unit
    ) {
        try {
            val currentFolder = connection.getStore().getFolder(folder)
            var result = serializer(message)
            if (!currentFolder.isOpen) {
                currentFolder.open(Folder.READ_WRITE)
            }
            currentFolder.appendMessages(arrayOf(result))
            cb(null)
        } catch (e: MessagingException) {
            cb(buildFailure(e))
        }
    }

    private fun serializer(message: MessageSpec): Message {
        val resultingMessage: Message = MimeMessage(connection.getSession())
        resultingMessage.subject = message.subject
        resultingMessage.setFrom(imapUserToAddress(message.sender))
        resultingMessage.setRecipients(Message.RecipientType.TO, userArrayToAddresses(message.toReceivers))
        resultingMessage.sentDate = message.timestamp.getDateValue()
        resultingMessage.setText(message.textBody)

        if (message.attachments.isNotEmpty()) {
            val multipart: Multipart = MimeMultipart()
            message.attachments.map { attachment ->
                val messageBodyPart = MimeBodyPart()
                messageBodyPart.fileName = attachment.title
                messageBodyPart.setContent(attachment.contentBase64, attachment.contentType)
                multipart.addBodyPart(messageBodyPart)
            }
            resultingMessage.setContent(multipart)
        }
        return resultingMessage
    }

    private fun userArrayToAddresses(imapUsers: YSArray<UserSpec>): Array<InternetAddress> {
        return imapUsers.map { imapUserToAddress(it) }.toTypedArray()
    }

    private fun imapUserToAddress(imapUser: UserSpec): InternetAddress {
        return InternetAddress(imapUser.email, imapUser.name)
    }
}

class DefaultImapProvider : ImapProvider {
    override fun provide(account: MailAccountSpec): Imap {
        return DefaultImap(account)
    }
}
