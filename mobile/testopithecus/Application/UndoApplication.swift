//
//  UndoApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 15.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public class UndoApplication: Undo {
    private let messageListPage = MessageListPage()

    public func undoArchive() throws {
        try XCTContext.runActivity(named: "Tapping on undo archiving button") { _ in
            guard self.isUndoArchiveToastShown() == UndoState.shown else {
                throw YSError("Undo archive toast is not shown")
            }
            try self.messageListPage.undoButton.tapCarefully()
        }
    }

    public func undoDelete() throws {
        try XCTContext.runActivity(named: "Tapping on undo deleting button") { _ in
            guard self.isUndoDeleteToastShown() == UndoState.shown else {
                throw YSError("Undo delete toast is not shown")
            }
            try self.messageListPage.undoButton.tapCarefully()
        }
    }

    public func undoSpam() throws {
        try XCTContext.runActivity(named: "Tapping on undo spam button") { _ in
            guard self.isUndoSpamToastShown() == UndoState.shown else {
                throw YSError("Undo spam toast is not shown")
            }
            try self.messageListPage.undoButton.tapCarefully()
        }
    }

    public func undoSending() throws {
        try XCTContext.runActivity(named: "Tapping on undo sending button") { _ in
            guard self.isUndoSpamToastShown() == UndoState.shown else {
                throw YSError("Undo sending toast is not shown")
            }
            try self.messageListPage.undoButton.tapCarefully()
        }
    }

    private func checkExistanceAndValidateUndoToast(label undoTitleLabel: String, andButton undoButtonLabel: String) -> UndoState {
        if self.messageListPage.undoView.exists &&
           self.messageListPage.undoTitle.label == undoTitleLabel &&
           self.messageListPage.undoButton.label == undoButtonLabel &&
           self.messageListPage.undoIcon.exists {
            return UndoState.shown
        } else {
            return UndoState.notShown
        }
    }

    public func isUndoArchiveToastShown() -> UndoState {
        XCTContext.runActivity(named: "Checking if undo archiving toast is shown") { _ in
            return self.checkExistanceAndValidateUndoToast(label: "Moved to archive", andButton: "Undo")
        }
    }

    public func isUndoDeleteToastShown() -> UndoState {
        XCTContext.runActivity(named: "Checking if undo deleting toast is shown") { _ in
            return self.checkExistanceAndValidateUndoToast(label: "Moved to Trash", andButton: "Undo")
        }
    }

    public func isUndoSpamToastShown() -> UndoState {
        XCTContext.runActivity(named: "Checking if undo spam toast is shown") { _ in
            return self.checkExistanceAndValidateUndoToast(label: "Marked as spam", andButton: "Undo")
        }
    }

    public func isUndoSendingToastShown() -> UndoState {
        XCTContext.runActivity(named: "Checking if undo sending toast is shown") { _ in
            return self.checkExistanceAndValidateUndoToast(label: "Send an email by accident?", andButton: "Cancel sending")
        }
    }
}
