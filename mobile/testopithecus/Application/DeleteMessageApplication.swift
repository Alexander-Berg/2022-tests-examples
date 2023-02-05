//
// Created by Elizaveta Y. Voronina on 11/15/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class DeleteMessageApplication: DeleteMessage {
    private let messageListPage = MessageListPage()

    public func deleteMessage(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Deleting \(order)-th message by long swipe left") { _ in
            try self.messageListPage.messageBy(index: Int(order)).longSwipeLeft()
        }
    }
}
