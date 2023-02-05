//
// Created by Artem I. Novikov on 13/02/2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class FilterNavigatorApplication: FilterNavigator {
    
    private let foldersListPage = FoldersListPage()
    private let messageListPage = MessageListPage()

    public func goToFilterImportant() throws {
        try XCTContext.runActivity(named: "Going to Important label") { _ in
            try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.important)
        }
    }

    public func goToFilterUnread() throws {
        try XCTContext.runActivity(named: "Going to Unread label") { _ in
            try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.unread)
        }
    }

    public func goToFilterWithAttachments() throws {
        try XCTContext.runActivity(named: "Going to label With attachments") { _ in
            try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: self.foldersListPage.withAttachments)
        }
    }
    
    public func getCurrentFilter() throws -> MessageContainerType! {
        return nil
    }
}
