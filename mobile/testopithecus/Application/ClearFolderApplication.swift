//
//  ClearFolderApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 04.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ClearFolderApplication: ClearFolderInFolderList {
    private let folderListPage = FoldersListPage()
    
    public func clearSpam(_ confirmDeletionIfNeeded: Bool) throws {
        try XCTContext.runActivity(named: "Clearing spam folder") { _ in
            try self.folderListPage.clearSpamFolderButton.tapCarefully()
            try dealWithConfirmationWindow(confirmDeletionIfNeeded, DefaultFolderName.spam)
        }
    }
    
    public func doesClearSpamButtonExist() throws -> Bool {
        if self.folderListPage.clearSpamFolderButton.exists {
            return self.folderListPage.clearSpamFolderButton.frame.width != self.folderListPage.spam.frame.width
        }
        return false
    }
    
    public func clearTrash(_ confirmDeletionIfNeeded: Bool) throws {
        try XCTContext.runActivity(named: "Clearing trash folder") { _ in
            try self.folderListPage.clearTrashFolderButton.tapCarefully()
            try dealWithConfirmationWindow(confirmDeletionIfNeeded, DefaultFolderName.trash)
        }
    }
    
    public func doesClearTrashButtonExist() throws -> Bool {
        if self.folderListPage.clearTrashFolderButton.exists {
            return self.folderListPage.clearTrashFolderButton.frame.width != self.folderListPage.trash.frame.width
        }
        return false
    }
    
    private func dealWithConfirmationWindow(_ confirmDeletionIfNeeded: Bool, _ folder: FolderName) throws {
        try XCTContext.runActivity(named: "Dealing with confirmation window") { _ in
            if confirmDeletionIfNeeded {
                guard let okButton = self.folderListPage.confirmClearFolder else {
                    throw YSError("There is no button to confirm clearing")
                }
                try okButton.tapCarefully()
                if folder == DefaultFolderName.trash {
                    self.folderListPage.clearTrashFolderButton.waitForAbsence()
                } else {
                    self.folderListPage.clearSpamFolderButton.waitForAbsence()
                }
            } else {
                guard let cancelButton = self.folderListPage.cancelClearFolder else {
                    throw YSError("There is no button to cancel clearing")
                }
                try cancelButton.tapCarefully()
            }
        }
    }
}
