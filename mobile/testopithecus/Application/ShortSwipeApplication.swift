//
// Created by Oleg Polyakov on 15/12/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public class ShortSwipeApplication: ShortSwipe {

    private let messageListPage = MessageListPage()

    public func deleteMessageByShortSwipe(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Deleting \(order)-th message by short swipe") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
            try self.messageListPage.swipeMenuDeleteButton.tapCarefully()
            if self.messageListPage.titleView.exists {
                if self.messageListPage.titleView.label == "Trash" {
                    guard let okButton = self.messageListPage.alertButtonOK else {
                        throw YSError("There is no OK button")
                    }
                    try okButton.tapCarefully()
                }
            }
        }
    }
    
    public func archiveMessageByShortSwipe(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Archiving \(order)-th message by short swipe") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
            try self.messageListPage.swipeMenuArchiveButton.tapCarefully()
        }
    }
    
    public func markAsRead(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking \(order)-th as read") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeRight()
        }
    }
    
    public func markAsUnread(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Marking \(order)-th as unread") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeRight()
        }
    }
}
