//
//  DefaultImap.swift
//  YandexMobileMailAutoTests
//
//  Created by Artur Kulsh on 2/11/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import PromiseKit
import testopithecus

public class DefaultImap: Imap {
    private let connection = MCOIMAPSession()

    public init(account: MailAccountSpec) {
        self.connection.authType = MCOAuthType.saslPlain
        self.connection.connectionType = MCOConnectionType.TLS
        self.connection.username = account.login
        self.connection.password = account.password
        self.connection.hostname = account.host
        self.connection.port = 993
        self.connection.timeout = 60_000
        self.connection.isCheckCertificateEnabled = false
        self.connection.connectionLogger = { _, _, data in
            if let data = data, let string = String(data: data, encoding: .utf8) {
                Log.info("Connection Logger: \(string)")
            }
        }
    }

    public func connect(_ cb: @escaping (YSError?) -> Void) {
        self.connection.connectOperation().start { cb(self.convert($0)) }
    }

    public func appendMessage(_ folder: String, _ message: MessageSpec, _ cb: @escaping (YSError?) -> Void ) {
        let bytes = self.serialize(message)
        let append = self.connection.appendMessageOperation(withFolder: folder, messageData: bytes, flags: MCOMessageFlag())
        append?.date = Int(message.timestamp.getDateValue().timeIntervalSince1970)
        append?.start { err, _ in
            cb(self.convert(err))
        }
    }

    public func createFolder(_ folder: String, _ cb: @escaping (YSError?) -> Void ) {
        self.connection.createFolderOperation(folder).start { cb(self.convert($0)) }
    }

    public func deleteMessages(_ folder: String, _ messageCount: Int32, _ cb: @escaping (YSError?) -> Void ) {
        let flags = MCOMessageFlag.deleted
        let kind = MCOIMAPStoreFlagsRequestKind.add
        let range = MCOIndexSet(range: MCORange(location: 1, length: UInt64(messageCount)))
        self.connection.storeFlagsOperation(withFolder: folder, numbers: range, kind: kind, flags: flags).start { cb(self.convert($0)) }
    }

    public func deleteFolder(_ folder: String, _ cb: @escaping (YSError?) -> Void ) {
        self.connection.deleteFolderOperation(folder).start { cb(self.convert($0)) }
    }

    public func openFolder(_ folder: String) -> XPromise<ImapFolderInfo> {
        promise { resolve, reject in
            self.connection.folderInfoOperation(folder).start { err, info in
                if let err = err {
                    reject(YSError("Could not get message count in folder \(folder): \(String(describing: err))"))
                    return
                }
                resolve(ImapFolderInfo(info!.messageCount))
            }
        }
    }

    public func expungeFolder(_ folder: String, _ cb: @escaping (YSError?) -> Void ) {
        self.connection.expungeOperation(folder).start { cb(self.convert($0)) }
    }

    public func fetchAllFolders() -> XPromise<YSArray<ImapFolderDisplay>> {
        promise { resolve, reject in
            self.connection.fetchAllFoldersOperation().start { err, folders in
                if let err = err {
                    reject(YSError("Could not get folders of this mailbox: \(err)"))
                    return
                }
                resolve(YSArray(array: folders!.map { $0 as! MCOIMAPFolder }.map { ImapFolderDisplay($0.path) }))
            }
        }
    }

    public func disconnect(_ cb: @escaping (YSError?) -> Void ) {
        self.connection.disconnectOperation().start { cb(self.convert($0)) }
    }

    private func convert(_ error: Error?) -> YSError? {
        if let error = error {
            return YSError("\(error)")
        }
        return nil
    }
    
    private func serialize(_ message: MessageSpec) -> Data {
        let messageBuilder = MCOMessageBuilder()
        messageBuilder.header.from = self.imapUserToMCOAddress(message.sender)
        messageBuilder.header.to = self.userArrayToAddressArray(message.toReceivers)
        messageBuilder.header.subject = message.subject
        messageBuilder.textBody = message.textBody
        for attachment in message.attachments {
            messageBuilder.addAttachment(self.attachToMCOAttachment(attachment))
        }
        return messageBuilder.data()!
    }

    private func userArrayToAddressArray(_ users: YSArray<UserSpec>) -> [MCOAddress] {
        var returnAddresses: [MCOAddress] = []
        var index = 0
        for user in users {
            returnAddresses[index] = self.imapUserToMCOAddress(user)
            index += 1
        }
        return returnAddresses
    }

    private func imapUserToMCOAddress(_ user: UserSpec) -> MCOAddress {
        return MCOAddress(displayName: user.name, mailbox: user.email)
    }

    private func attachToMCOAttachment(_ attachment: AttachmentSpec) -> MCOAttachment {
        let attach = MCOAttachment()
        attach.mimeType = attachment.contentType
        attach.filename = attachment.title
        attach.data = Data(base64Encoded: attachment.contentBase64)
        return attach
    }
}

public class DefaultImapProvider: ImapProvider {
    public func provide(_ account: MailAccountSpec) -> Imap {
        DefaultImap(account: account)
    }
}
