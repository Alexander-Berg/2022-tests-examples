//
//  LongSwipeApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 25.02.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public class LongSwipeApplication: LongSwipe {
    private let messageListPage = MessageListPage()
    
    public func deleteMessageByLongSwipe(_ order: Int32, _ confirmDeletionIfNeeded: Bool) throws {
        try XCTContext.runActivity(named: "Deleting \(order)-th message by long swipe left") { _ in
            try self.messageListPage.messageBy(index: Int(order)).longSwipeLeft()
            if self.messageListPage.titleView.exists && self.messageListPage.titleView.label == "Trash" {
                if confirmDeletionIfNeeded {
                    guard let okButton = self.messageListPage.alertButtonOK else {
                        throw YSError("There is no OK button")
                    }
                    try okButton.tapCarefully()
                } else {
                    guard let cancelButton = self.messageListPage.alertButtonCancel else {
                        throw YSError("There is no Cancel button")
                    }
                    try cancelButton.tapCarefully()
                }
            }
        }
    }
    
    public func archiveMessageByLongSwipe(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Archiving \(order)-th message by long swipe left") { _ in
            try self.messageListPage.messageBy(index: Int(order)).longSwipeLeft()
        }
    }
}
