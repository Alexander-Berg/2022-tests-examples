//
// Created by Fedor Amosov on 09/09/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class MarkableApplication: MarkableRead {
    private let messageListPage = MessageListPage()

    public func markAsRead(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking as read \(order)-th message by short swipe right") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeRight()
        }
    }

    public func markAsUnread(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking as unread \(order)-th message by short swipe right") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeRight()
        }
    }
}
