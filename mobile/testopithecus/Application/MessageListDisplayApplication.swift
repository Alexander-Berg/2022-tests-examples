//
// Created by Artem I. Novikov on 18/10/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class MessageListDisplayApplication: MessageListDisplay {
    private let messageListPage = MessageListPage()

    public func getMessageList(_ limit: Int32) -> YSArray<MessageView> {
        XCTContext.runActivity(named: "Getting messages from message list") { _ in
            self.messageListPage.loader.shouldAbsent(timeout: 5)
            let messages: [MessageView] = self.messageListPage.loadedMessages.prefix(Int(limit)).compactMap { messageElement in
                let from = messageElement.from.label
                let subject = messageElement.subject.label
                let unread = messageElement.unreadMarker.exists
                let firstLine = messageElement.firstLine.label
                let important = messageElement.important.exists
                let threadCounter = messageElement.threadCounter.exists ? Int32(messageElement.threadCounter.label)! : nil
                let attachments: YSArray<AttachmentView> = YSArray(array: messageElement.attachments.map { MessageAttach($0.name.label) })
                return Message(from, subject, getTimestamp(dateString: messageElement.date.label), firstLine, threadCounter, !unread, important, attachments)
            }
            return YSArray(array: messages)
        }
    }

    public func refreshMessageList() throws {
        try XCTContext.runActivity(named: "Refreshing message list") { _ in
            try self.messageListPage.tableView.shouldExist().swipeDown()
            sleep(2)
        }
    }

    public func swipeDownMessageList() throws {
        try XCTContext.runActivity(named: "Scroll to load more messages of endless list") { _ in
            try self.messageListPage.tableView.shouldExist().swipeUp()
            sleep(2)
        }
    }

    public func unreadCounter() -> Int32 {
        XCTContext.runActivity(named: "Getting unread messages counter") { _ in
            let unreadCounter = self.messageListPage.navigationBarUnreadCounter
            return unreadCounter.exists ? Int32(unreadCounter.label)! : 0
        }
    }

    private func getTimestamp(dateString: String) -> Int64 {
        XCTContext.runActivity(named: "Getting timestamp") { _ in
            let dateFormatter = DateFormatter()
            dateFormatter.locale = Locale(identifier: "en_US_POSIX") // set locale to reliable US_POSIX
            var date: Date?
            for dateFormat in ["MMM d", "h:mm a", "hh:mm"] where date == nil {
                dateFormatter.dateFormat = dateFormat
                date = dateFormatter.date(from: dateString)
            }
            return doubleToInt64(date!.timeIntervalSince1970 * 1000)
        }
    }
}
