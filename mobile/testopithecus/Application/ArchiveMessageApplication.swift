//
// Created by Oleg Polyakov on 15/12/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public class ArchiveMessageApplication: ArchiveMessage {
    private let messageListPage = MessageListPage()

    public func archiveMessage(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Archiving \(order)-th message from short short swipe menu") { _ in
            try self.messageListPage.messageBy(index: Int(order)).swipeLeft()
            try self.messageListPage.swipeMenuMoreButton.tapCarefully()
            try self.messageListPage.messageActionsArchiveButton.tapCarefully()
        }
    }
}
