//
// Created by Artem I. Novikov on 12/11/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class GroupModeApplication: GroupMode {
    private let messageListPage = MessageListPage()
    private let moveToFolderPage = MoveToFolderPage()
    private let applyLabelPage = ApplyLabelPage()
    private static var currentFolder = ""

    public func isInGroupMode() throws -> Bool {
        try XCTContext.runActivity(named: "Checking if messages list is in group mode") { _ in
            return try self.isGroupModePanelExists() &&
                self.messageListPage.selectAllNavigationButton.exists &&
                self.messageListPage.cancelSelectingNavigationButton.exists &&
                self.messageListPage.selectionTitleNavigationButton.exists
        }
    }

    private func isGroupModePanelExists() throws -> Bool {
        XCTContext.runActivity(named: "Checking if group mode panel is shown") { _ in
            self.messageListPage.groupOperationPanel.yo_waitForExistence(timeout: 1)
        }
    }

    public func markAsRead() throws {
        try XCTContext.runActivity(named: "Marking as read selected message(s)") { _ in
            try self.messageListPage.groupOperationMarkAsRead.tapCarefully()
        }
    }

    public func markAsUnread() throws {
        try XCTContext.runActivity(named: "Marking as unread selected message(s)") { _ in
            try self.messageListPage.groupOperationMarkAsUnread.tapCarefully()
        }
    }

    public func delete() throws {
        try XCTContext.runActivity(named: "Deleting selected message(s)") { _ in
            try self.messageListPage.groupOperationDelete.tapCarefully()
            if Self.currentFolder == "Trash" {
                guard let okButton = self.messageListPage.alertButtonOK else {
                    throw YSError("There is no OK button")
                }
                try okButton.tapCarefully()
            }
        }
    }

    public func getNumberOfSelectedMessages() throws -> Int32 {
        try XCTContext.runActivity(named: "Getting number of selected messages") { _ in
            guard self.messageListPage.selectionTitleNavigationButton.exists else {
                throw YSError("Selection title is not shown")
            }
            guard let numberOfSelectedMessages = Int32(self.messageListPage.selectionTitleNavigationButton.value as! String) else {
                throw YSError("There is no numeric numberOfSelectedMessages")
            }
            return numberOfSelectedMessages
        }
    }

    public func selectAllMessages() throws {
        try XCTContext.runActivity(named: "Selecting all messages in group mode") { _ in
            try self.messageListPage.selectAllNavigationButton.tapCarefully()
        }
    }

    public func selectMessage(_ byOrder: Int32) throws {
        try XCTContext.runActivity(named: "Selecting \(byOrder)-th message in group mode") { _ in
            try self.messageListPage.messageBy(index: Int(byOrder)).subject.tapCarefully()
        }
    }

    public func initialMessageSelect(_ byOrder: Int32) throws {
        try XCTContext.runActivity(named: "Activating group mode by selecting \(byOrder)-th message") { _ in
            Self.currentFolder = self.messageListPage.titleView.exists ? self.messageListPage.titleView.label : ""
            var retriesCount = 0
            while !(try self.isGroupModePanelExists()) && retriesCount < 3 {
                try self.messageListPage.messageBy(index: Int(byOrder)).avatarTapZone.tapCarefully()
                retriesCount += 1
            }
        }
    }

    public func getSelectedMessages() -> YSSet<Int32> {
        XCTContext.runActivity(named: "Getting selected messages") { _ in
            return YSSet(self.messageListPage.loadedMessages.enumerated().filter {
                $0.element.selectOnAvatar.exists
            }.map {
                Int32($0.offset)
            })
        }
    }

    public func markAsImportant() throws {
        try XCTContext.runActivity(named: "Marking as important selected messages") { _ in
            if !self.messageListPage.groupOperationMarkAsImportant.isHittable {
                try self.messageListPage.groupOperationShowMore.tapCarefully()
            }
            try self.messageListPage.groupOperationMarkAsImportant.tapCarefully()
        }
    }

    public func markAsUnimportant() throws {
        try XCTContext.runActivity(named: "Marking as unimportant selected messages") { _ in
            try self.messageListPage.groupOperationMarkAsNotImportant.tapCarefully()
        }
    }

    public func markAsSpam() throws {
        try XCTContext.runActivity(named: "Marking as spam selected messages") { _ in
            try self.messageListPage.groupOperationMoveToSpam.tapCarefully()
        }
    }

    public func markAsNotSpam() throws {
        try XCTContext.runActivity(named: "Marking as not spam selected messages") { _ in
            try self.messageListPage.groupOperationNotSpam.tapCarefully()
        }
    }

    public func archive() throws {
        try XCTContext.runActivity(named: "Archiving selected messages") { _ in
            if UIDevice.current.userInterfaceIdiom == .phone {
                try self.messageListPage.groupOperationShowMore.tapCarefully()
            }
            try self.messageListPage.groupOperationArchive.tapCarefully()
        }
    }

    public func unselectMessage(_ byOrder: Int32) throws {
        try XCTContext.runActivity(named: "Deselecting \(byOrder)-th message") { _ in
            try self.messageListPage.messageBy(index: Int(byOrder)).subject.tapCarefully()
        }
    }

    public func unselectAllMessages() throws {
        try XCTContext.runActivity(named: "Deselecting all messages") { _ in
            try self.messageListPage.cancelSelectingNavigationButton.tapCarefully()
        }
    }
    
    public func openApplyLabelsScreen() throws {
        try XCTContext.runActivity(named: "Opening Apply label screen") { _ in
            if UIDevice.current.userInterfaceIdiom == .phone {
                try self.messageListPage.groupOperationShowMore.tapCarefully()
            }
            try self.messageListPage.groupOperationApplyLabel.tapCarefully()
            self.applyLabelPage.tableView.yo_waitForExistence()
        }
    }
    
    public func openMoveToFolderScreen() throws {
        try XCTContext.runActivity(named: "Opening Move to folder screen") { _ in
            if UIDevice.current.userInterfaceIdiom == .phone {
                try self.messageListPage.groupOperationShowMore.tapCarefully()
            }
            try self.messageListPage.groupOperationMoveToFolder.tapCarefully()
            try self.moveToFolderPage.tableView.shouldExist()
        }
    }
}
