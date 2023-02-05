//
// Created by Artem Zoshchuk on 04.03.2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class SearchApplication: Search {
    private let messageList = MessageListPage()
    private let searchPage = InstantSearchPage()

    public func searchAllMessages() throws {
        try XCTContext.runActivity(named: "Searching all messages") { _ in
            try self.searchByQuery("yandex")
            self.messageList.tableView.yo_waitForExistence(timeout: 4.0)
        }
    }

    public func closeSearch() throws {
        try XCTContext.runActivity(named: "Closing search screen") { _ in
            try self.searchPage.searchBarBackButton.tapCarefully()
        }
    }

    public func clearTextField() throws {
        try XCTContext.runActivity(named: "Clearing search field") { _ in
            try self.searchPage.searchBarClearButton.tapCarefully()
        }
    }

    public func isInSearch() -> Bool {
        XCTContext.runActivity(named: "Checking if is in search mode") { _ in
            self.searchPage.searchBarTextField.yo_waitForExistence()
        }
    }

    // нужно для расширенного поиска, т.к. им можно воспользоваться только если поискал сначала обычный запрос, в iOS
    // расширенного поиска нет, поэтому отдаём true просто если открыт поиск
    public func isSearchedForMessages() -> Bool {
        XCTContext.runActivity(named: "Checking if on a search result screen") { _ in
            self.searchPage.searchBarTextField.exists
        }
    }

    public func openSearch() throws {
        try XCTContext.runActivity(named: "Opening search") { _ in
            var retriesCount = 0
            while !self.isInSearch() && retriesCount < 3 {
                try self.messageList.searchNavigationBarButton.tapCarefully()
                retriesCount += 1
            }
        }
    }

    public func searchByQuery(_ request: String) throws {
        XCTContext.runActivity(named: "Searching for messages using the \"\(request)\" search query") { _ in
            self.searchPage.searchBarTextField.typeText(request)
            messageList.keyboard.buttons["Search"].tap()
        }
    }
}
