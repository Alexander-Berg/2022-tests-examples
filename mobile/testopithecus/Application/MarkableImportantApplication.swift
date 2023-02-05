//
// Created by Artem I. Novikov on 16/10/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class MarkableImportantApplication: MarkableImportant {
    private let messageListPage = MessageListPage()

    public func markAsImportant(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking as important \(order)-th message from short swipe menu") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
            try self.messageListPage.swipeMenuMoreButton.tapCarefully()
            try self.messageListPage.messageActionsImportantButton.tapCarefully()
        }
    }

    public func markAsUnimportant(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking as unimportant \(order)-th message from short swipe menu") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
            try self.messageListPage.swipeMenuMoreButton.tapCarefully()
            try self.messageListPage.messageActionsNotImportantButton.tapCarefully()
        }
    }
}
