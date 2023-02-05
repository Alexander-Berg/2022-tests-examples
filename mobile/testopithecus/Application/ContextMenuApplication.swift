//
// Created by Elizaveta Y. Voronina on 11/19/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ContextMenuApplication: ContextMenu {
    private let messageListPage = MessageListPage()
    private let moveToFolderPage = MoveToFolderPage()
    private let messageActionsPage = MessageActionsPage()
    private let applyLabelPage = ApplyLabelPage()
    private let messageViewPage = MessageViewPage()
    
    public func openFromShortSwipe(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Opening context menu from short swipe") { _ in
            var retriesCount = 0
            while !self.isSwipeMenuOpened() && retriesCount < 3 {
                try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
                retriesCount += 1
            }
            try self.messageListPage.swipeMenuMoreButton.tapCarefully()
        }
    }
    
    public func openFromMessageView() throws {
        try XCTContext.runActivity(named: "Opening context menu from message view") { _ in
            try self.messageViewPage.messageHeaderControlMore.tapCarefully()
        }
    }
    
    public func close() throws {
        try XCTContext.runActivity(named: "Closing context menu") { _ in
            try self.messageActionsPage.headerCloseButton.tapCarefully()
        }
    }

    public func deleteMessage() throws {
        try XCTContext.runActivity(named: "Deleting message from short swipe menu") { _ in
            try self.messageActionsPage.delete.tapCarefully()
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

    public func markAsRead() throws {
        try XCTContext.runActivity(named: "Marking message as read from short swipe menu") { _ in
            try self.messageActionsPage.markAsRead.tapCarefully()
        }
    }

    public func markAsUnread() throws {
        try XCTContext.runActivity(named: "Marking message as unread from short swipe menu") { _ in
            try self.messageActionsPage.markAsUnread.tapCarefully()
        }
    }

    public func markAsImportant() throws {
        try XCTContext.runActivity(named: "Marking message as important from short swipe menu") { _ in
            try self.messageActionsPage.markAsImportant.tapCarefully()
        }
    }

    public func markAsUnimportant() throws {
        try XCTContext.runActivity(named: "Marking message message as unimportant from short swipe menu") { _ in
            try self.messageActionsPage.markAsNotImportant.tapCarefully()
        }
    }

    public func archive() throws {
        try XCTContext.runActivity(named: "Archiving message from short swipe menu") { _ in
            try self.messageActionsPage.archive.tapCarefully()
        }
    }

    private func isSwipeMenuOpened() -> Bool {
        return self.messageListPage.swipeMenuMoreButton.yo_waitForExistence()
    }

    public func openReplyCompose() throws {
        try XCTContext.runActivity(named: "Opening compose Reply to message from short swipe menu") { _ in
            try self.messageActionsPage.reply.tapCarefully()
        }
    }

    public func openReplyAllCompose() throws {
        try XCTContext.runActivity(named: "Opening compose Reply All to message from short swipe menu") { _ in
            try self.messageActionsPage.replyAll.tapCarefully()
        }
    }

    public func openForwardCompose() throws {
        try XCTContext.runActivity(named: "Opening compose Forward to message from short swipe menu") { _ in
            try self.messageActionsPage.forward.tapCarefully()
        }
    }

    public func markAsSpam() throws {
        try XCTContext.runActivity(named: "Marking message as spam from short swipe menu") { _ in
            try self.messageActionsPage.spam.tapCarefully()
        }
    }

    public func markAsNotSpam() throws {
        try XCTContext.runActivity(named: "Marking message as not spam from short swipe menu") { _ in
            try self.messageActionsPage.notSpam.tapCarefully()
        }
    }
    
    public func openApplyLabelsScreen() throws {
        try XCTContext.runActivity(named: "Opening Apply label screen") { _ in
            try self.messageActionsPage.applyLabel.tapCarefully()
            self.applyLabelPage.tableView.yo_waitForExistence()
        }
    }
    
    public func openMoveToFolderScreen() throws {
        try XCTContext.runActivity(named: "Opening Move to folder screen") { _ in
            try self.messageActionsPage.moveToFolder.tapCarefully()
            try self.moveToFolderPage.tableView.shouldExist()
        }
    }
    
    public func getAvailableActions() throws -> YSArray<MessageActionName> {
        XCTContext.runActivity(named: "Getting available actions") { _ in
            return YSArray(array: self.messageActionsPage.availableActions)
        }
    }
    
    public func showTranslator() throws {
        try XCTContext.runActivity(named: "Tapping on Show translator button") { _ in
            try self.messageActionsPage.translate.tapCarefully()
        }
    }
}
