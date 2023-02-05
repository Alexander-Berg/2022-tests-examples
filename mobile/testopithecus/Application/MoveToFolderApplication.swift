//
//  MoveToFolderApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 02.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class MoveToFolderApplication: MoveToFolder {
    private let moveToFolderPage = MoveToFolderPage()
    
    public func tapOnFolder(_ folderName: FolderName) throws {
        try XCTContext.runActivity(named: "Moving message to folder \(folderName)") { _ in
            guard let folderToMove = self.moveToFolderPage.findFolderByName(folderName) else {
                throw YSError("Unable to find folder with name \"\(folderName)\"")
            }
            try folderToMove.tapCarefully()
        }
    }
    
    public func tapOnCreateFolder() throws {
        try XCTContext.runActivity(named: "Tap on FAB") { _ in
            try self.moveToFolderPage.fab.tapCarefully()
        }
    }
    
    public func getFolderList() throws -> YSArray<FolderName> {
        XCTContext.runActivity(named: "Getting folder list") { _ in
            return YSArray(array: self.moveToFolderPage.foldersNameList)
        }
    }
}
