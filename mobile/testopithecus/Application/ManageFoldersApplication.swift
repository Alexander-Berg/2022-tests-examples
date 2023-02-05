//
//  ManageFoldersApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 01.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ManageFoldersApplication: ManageableFolder {
    private let messageListPage = MessageListPage()
    private let foldersListPage = FoldersListPage()
    private let manageFoldersPage = ManageFoldersPage()
    private let addFolderPage = AddFolderPage()
    private let editFolderPage = EditFolderPage()
    private let selectParentFolderPage = SelectParentFolderPage()
    
    public func openFolderManager() throws {
        try XCTContext.runActivity(named: "Opening folder manager") { _ in
            try scrollIfNeeded(view: self.foldersListPage.view, to: self.foldersListPage.currentAccountEmailLabel)
            try self.foldersListPage.manageFolders.forceTap()
            self.manageFoldersPage.tableView.yo_waitForExistence()
        }
    }
    
    public func closeFolderManager() throws {
        try XCTContext.runActivity(named: "Closing folder manager") { _ in
            try self.manageFoldersPage.closeButton.tapCarefully()
            self.foldersListPage.view.yo_waitForExistence()
        }
    }
    
    public func deleteFolder(_ folderDisplayName: FolderName, _ parentFolders: YSArray<FolderName>, _ deletionMethod: ContainerDeletionMethod) throws {
        try XCTContext.runActivity(named: "Deleting folder") { _ in
            try self.expandParentFoldersIfNeeded(parentFolders)
            let folder = self.manageFoldersPage.findFolder(withName: folderDisplayName)
            
            switch deletionMethod {
            case .longSwipe:
                try folder.longSwipe(.left)
            case .shortSwipe:
                try folder.swipeLeft()
                try folder.deleteRightButton.tapCarefully()
            case .tap:
                try self.manageFoldersPage.editButton.tapCarefully()
                try folder.deleteLeftButton.tapCarefully()
                try folder.deleteRightButton.tapCarefully()
            }
            
            try self.manageFoldersPage.alertOKButton.tapCarefully()
            
            if self.manageFoldersPage.doneButton.yo_waitForExistence() {
                try self.manageFoldersPage.doneButton.tapCarefully()
            }
        }
    }
    
    public func isFolderManagerOpened() -> Bool {
        XCTContext.runActivity(named: "Checking is folder manager opened") { _ in
            return self.manageFoldersPage.tableView.yo_waitForExistence()
        }
    }
    
    public func getFolderList() throws -> YSArray<FolderName> {
        try XCTContext.runActivity(named: "Getting folder list") { _ in
            let expandableFolder: [ManageableFolderElement] = self.manageFoldersPage.expandableFolders
            try self.expand(folders: expandableFolder)
            return YSArray(array: self.manageFoldersPage.folderNameList.map { folderName in FolderName(folderName) })
        }
    }
    
    private func expandParentFoldersIfNeeded(_ parentFolders: YSArray<FolderName>) throws {
        if parentFolders.length > 0 {
            for parentFolder in parentFolders {
                try self.manageFoldersPage.expandButtonForFolder(withName: parentFolder).tapCarefully()
            }
        }
    }
    
    public func openCreateFolderScreen() throws {
        try XCTContext.runActivity(named: "Opening create folder screen") { _ in
            try self.manageFoldersPage.addButton.tapCarefully()
            self.addFolderPage.formView.yo_waitForExistence()
        }
    }
    
    public func closeCreateFolderScreen() throws {
        try XCTContext.runActivity(named: "Closing create folder screen") { _ in
            try self.addFolderPage.closeButton.tapCarefully()
            self.manageFoldersPage.tableView.yo_waitForExistence()
        }
    }
    
    public func enterNameForNewFolder(_ folderName: FolderName) {
        XCTContext.runActivity(named: "Entering name for new folder") { _ in
            self.addFolderPage.formTextField.typeText(folderName)
        }
    }
    
    public func getCurrentNewFolderName() -> FolderName {
        XCTContext.runActivity(named: "Getting current folder name") { _ in
            return FolderName(self.addFolderPage.formTextField.value as! String)
        }
    }
    
    public func getCurrentParentFolderForNewFolder() -> String {
        XCTContext.runActivity(named: "Getting current parent folder for new folder") { _ in
            return formatAccountEmail(login: self.addFolderPage.parentFolderName.label)
        }
    }
    
    public func submitNewFolder() throws {
        try XCTContext.runActivity(named: "Submitting new folder") { _ in
            try self.addFolderPage.doneButton.tapCarefully()
            self.manageFoldersPage.tableView.yo_waitForExistence()
        }
    }
    
    public func openEditFolderScreen(_ folderName: FolderName, _ parentFolders: YSArray<FolderName>) throws {
        try XCTContext.runActivity(named: "Opening edit folder screen for \(folderName)") { _ in
            try self.expandParentFoldersIfNeeded(parentFolders)
            try self.manageFoldersPage.findFolder(withName: folderName).tap()
            
            self.editFolderPage.formView.yo_waitForExistence()
        }
    }
    
    public func closeEditFolderScreen() throws {
        try XCTContext.runActivity(named: "Closing edit folder screen") { _ in
            try self.editFolderPage.backButton.tapCarefully()
            self.manageFoldersPage.tableView.yo_waitForExistence()
        }
    }
    
    public func enterNameForEditedFolder(_ folderName: FolderName) {
        XCTContext.runActivity(named: "Entering new name for folder - \(folderName)") { _ in
            self.editFolderPage.formTextField.clearField()
            self.editFolderPage.formTextField.typeText(folderName)
        }
    }
    
    public func getCurrentEditedFolderName() -> FolderName {
        XCTContext.runActivity(named: "Getting current edited folder name") { _ in
            return FolderName(self.editFolderPage.formTextField.value as! String)
        }
    }
    
    public func getCurrentParentFolderForEditedFolder() -> String {
        XCTContext.runActivity(named: "Getting current parent folder for edited folder") { _ in
            return formatAccountEmail(login: self.editFolderPage.parentFolderName.label)
        }
    }
    
    public func submitEditedFolder() throws {
        try XCTContext.runActivity(named: "Apply folder changes") { _ in
            try self.editFolderPage.doneButton.tapCarefully()
        }
    }
    
    public func openFolderLocationScreen() throws {
        try XCTContext.runActivity(named: "Opening folder location screen") { _ in
            if self.editFolderPage.title.exists && self.editFolderPage.title.label == "Edit folder" {
                try self.editFolderPage.locationView.tapCarefully()
            } else if self.addFolderPage.title.exists && self.addFolderPage.title.label == "New folder" {
                try self.addFolderPage.locationView.tapCarefully()
            }
            self.selectParentFolderPage.tableView.yo_waitForExistence()
        }
    }
    
    public func getFolderListForFolderLocationScreen() throws -> YSArray<String> {
        try XCTContext.runActivity(named: "Getting folder list at location screen") { _ in
            let expandableFolder: [SelectableParentFolderElement] = self.selectParentFolderPage.expandableFolders
            try self.expand(folders: expandableFolder)
            let folderNameList: [String] = self.selectParentFolderPage.folderNameList.map { folderName in
                FolderName(folderName)
            } + [formatAccountEmail(login: FolderName(self.selectParentFolderPage.accountName.label))]
            return YSArray(array: folderNameList)
        }
    }
    
    public func closeFolderLocationScreen() throws {
        try XCTContext.runActivity(named: "Closing folder location screen") { _ in
            try self.selectParentFolderPage.backButton.tapCarefully()
        }
    }
    
    public func getFolderListForManageFolderScreen() throws -> YSArray<FolderName> {
        try XCTContext.runActivity(named: "Getting folder list at manage folder screen") { _ in
            let expandableFolder: [ManageableFolderElement] = self.manageFoldersPage.expandableFolders
            try self.expand(folders: expandableFolder)
            return YSArray(array: self.manageFoldersPage.folderNameList.map { folderName in FolderName(folderName) })
        }
    }
    
    public func selectParentFolder(_ parentFolders: YSArray<FolderName>) throws {
        try XCTContext.runActivity(named: "Selecting parent folder") { _ in
            if parentFolders.length == 1 {
                try self.selectParentFolderPage.findFolder(withName: parentFolders[0]).tap()
            } else {
                let expandableParentFolders = parentFolders.dropLast()
                for parentFolder in expandableParentFolders {
                    try self.selectParentFolderPage.expandButtonForFolder(withName: parentFolder).tapCarefully()
                }
                try self.selectParentFolderPage.findFolder(withName: parentFolders[parentFolders.length - 1]).tap()
            }
        }
    }
    
    private func expand(folders expandableFolders: [SelectableParentFolderElement], _ expandedFolders: [String] = []) throws {
        guard !expandableFolders.isEmpty else { return }
        
        try expandableFolders[0].expandSubfolders()
        let newExpandedFolders = expandedFolders + [expandableFolders[0].folderName.label]
        let newExpandableFolders = self.selectParentFolderPage.expandableFolders.filter {
            !newExpandedFolders.contains($0.folderName.label)
        }

        try expand(folders: newExpandableFolders, newExpandedFolders)
    }
    
    private func expand(folders expandableFolders: [ManageableFolderElement], _ expandedFolders: [String] = []) throws {
        guard !expandableFolders.isEmpty else { return }
        
        try expandableFolders[0].expandSubfolders()
        let newExpandedFolders = expandedFolders + [expandableFolders[0].folderName.label]
        let newExpandableFolders = self.manageFoldersPage.expandableFolders.filter {
            !newExpandedFolders.contains($0.folderName.label)
        }

        try expand(folders: newExpandableFolders, newExpandedFolders)
    }
}
