//
//  FolderNavigatorApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Elizaveta Y. Voronina on 10/16/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import Foundation
import testopithecus

public final class FolderNavigatorApplication: FolderNavigator {
        
    private let messageListPage = MessageListPage()
    private let foldersListPage = FoldersListPage()
    
    public func getCurrentContainer() throws -> String! {
        XCTContext.runActivity(named: "Getting current folder name") { _ in
            let currentContainer = self.foldersListPage.currentContainer
            if currentContainer != nil {
                return self.foldersListPage.getContainerName(container: currentContainer!)
            }
            return nil
        }
    }
    
    public func ptrFoldersList() throws {
        XCTContext.runActivity(named: "Executing PTR in folder lost") { _ in
            self.foldersListPage.view.swipeDown()
        }
    }

    public func getFoldersList() throws -> YSMap<FolderName, Int32> {
        try XCTContext.runActivity(named: "Getting user's folders list") { _ in
            let folderList: YSMap<FolderName, Int32> = YSMap()
            
            if self.foldersListPage.inbox.exists {
                folderList.set(DefaultFolderName.inbox, try getFolderCounter(folder: self.foldersListPage.inbox))
            }
            
            if self.foldersListPage.socialTab.exists {
                folderList.set(DefaultFolderName.socialNetworks, try getFolderCounter(folder: self.foldersListPage.socialTab))
            }
            
            if self.foldersListPage.newsTab.exists {
                folderList.set(DefaultFolderName.mailingLists, try getFolderCounter(folder: self.foldersListPage.newsTab))
            }
            
            let expandableFolder: [UserFolderElement] = self.foldersListPage.userFolders.filter { $0.isExpandable }
            try self.expand(folders: expandableFolder)
            try self.foldersListPage.userFolders.forEach { userFolder in
                folderList.set(userFolder.folderName.label, try getFolderCounter(folder: userFolder.container))
            }
            
            if self.foldersListPage.sent.exists {
                folderList.set(DefaultFolderName.sent, try getFolderCounter(folder: self.foldersListPage.sent))
            }
            if self.foldersListPage.trash.exists {
                folderList.set(DefaultFolderName.trash, try getFolderCounter(folder: self.foldersListPage.trash))
            }
            if self.foldersListPage.spam.exists {
                folderList.set(DefaultFolderName.spam, try getFolderCounter(folder: self.foldersListPage.spam))
            }
            if self.foldersListPage.draft.exists {
                folderList.set(DefaultFolderName.draft, try getFolderCounter(folder: self.foldersListPage.draft))
                if self.foldersListPage.draftExpandButton.exists {
                    try self.foldersListPage.draftExpandButton.tapCarefully()
                    folderList.set(DefaultFolderName.template, try getFolderCounter(folder: self.foldersListPage.templates))
                }
            }
            if self.foldersListPage.archive.exists {
                folderList.set(DefaultFolderName.archive, try getFolderCounter(folder: self.foldersListPage.archive))
            }
            return folderList
        }
    }
    
    public func openFolderList() throws {
        try XCTContext.runActivity(named: "Opening folder list") { _ in
            self.messageListPage.burgerNavigationBarButton.yo_waitForExistence(timeout: 5)
            try self.messageListPage.burgerNavigationBarButton.tapCarefully()
            self.foldersListPage.tableView.yo_waitForExistence()
        }
    }
    
    public func closeFolderList() throws {
        try XCTContext.runActivity(named: "Closing folder list") { _ in
            try self.foldersListPage.view.shouldExist().swipeLeft()
        }
    }

    public func isInTabsMode() -> Bool {
        return self.foldersListPage.socialTab.exists && self.foldersListPage.newsTab.exists
    }

    private func getFolderElementIfExist(_ folderName: String) throws -> UserFolderElement {
        guard let folder = self.foldersListPage.findUserFolderByName(String(folderName)) else {
            throw YSError("Unable to find user folder with name \"\(folderName)\"")
        }
        return folder
    }

    public func goToFolder(_ folderDisplayName: String, _ parentFolders: YSArray<String>) throws {
        try XCTContext.runActivity(named: "Going to folder with name \(folderDisplayName)") { _ in
            switch folderDisplayName {
            case DefaultFolderName.inbox:
                try scrollIfNeeded(view: self.foldersListPage.view, to: self.foldersListPage.accountSwitcherScrollView)
                try self.foldersListPage.inbox.tapCarefully()
            case DefaultFolderName.socialNetworks:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.socialTab)
            case DefaultFolderName.mailingLists:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.newsTab)
            case DefaultFolderName.sent:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.sent)
            case DefaultFolderName.trash:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.trash)
            case DefaultFolderName.spam:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.spam)
            case DefaultFolderName.archive:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.archive)
            case DefaultFolderName.draft:
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.draft)
            case DefaultFolderName.template:
                if !self.foldersListPage.templates.exists {
                    try scrollIfNeeded(view: self.foldersListPage.view, to: self.foldersListPage.draft)
                    if self.foldersListPage.draftExpandButton.exists {
                        try self.foldersListPage.draftExpandButton.tapCarefully()
                    }
                }
                try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.templates)
            default:
                if parentFolders.length > 0 {
                    for parentFolder in parentFolders {
                        let folderToExpand = try getFolderElementIfExist(parentFolder)
                        try scrollIfNeeded(view: self.foldersListPage.view, to: folderToExpand.expandButton)
                        try folderToExpand.expandSubfolders()
                    }
                }
                let userFolder = try getFolderElementIfExist(folderDisplayName)
                try userFolder.tap()
            }
            self.messageListPage.tableView.yo_waitForExistence(timeout: 4.0)
        }
    }
    
    private func expand(folders expandableFolders: [UserFolderElement], _ expandedFolders: [String] = []) throws {
        guard !expandableFolders.isEmpty else { return }
        
        try expandableFolders[0].expandSubfolders()
        let newExpandedFolders = expandedFolders + [expandableFolders[0].folderName.label]
                
        let newExpandableFolders = self.foldersListPage.userExpandableFolders.filter {
            !newExpandedFolders.contains($0.folderName.label)
        }

        try expand(folders: newExpandableFolders, newExpandedFolders)
    }
    
    private func getFolderCounter(folder: XCUIElement) throws -> Int32 {
        let counterElement = UserFolderElement(container: folder).unreadCounter
        if !counterElement.exists {
            return 0
        }
        let counter = counterElement.label
        if counter.length > 0 {
            return Int32(counter)!
        }
        return 0
    }
}
