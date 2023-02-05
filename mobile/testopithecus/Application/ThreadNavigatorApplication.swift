//
//  ThreadNavigatorApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 02.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class ThreadNavigatorApplication: ThreadViewNavigator {
    private let messageViewPage = MessageViewPage()
    
    public func deleteCurrentThread() throws {
        try XCTContext.runActivity(named: "Deleting current thread from nav bar") {_ in
            try self.messageViewPage.deleteButton.tapCarefully()
        }
    }
    
    public func archiveCurrentThread() throws {
        try XCTContext.runActivity(named: "Archiving current thread from nav bar") {_ in
            try self.messageViewPage.archiveButton.tapCarefully()
        }
    }
}
