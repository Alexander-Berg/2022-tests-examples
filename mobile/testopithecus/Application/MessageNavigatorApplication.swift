//
// Created by Artem I. Novikov on 20/11/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class MessageNavigatorApplication: MessageViewer {
    private let messageListPage = MessageListPage()
    private let messageViewPage = MessageViewPage()
    
    private var keepDetailsOpen = false

    public func openMessage(_ order: Int32) throws {
        try XCTContext.runActivity(named: "Opening \(order)-th message") { _ in
            try self.messageListPage.messageBy(index: Int(order)).tap()
            self.messageViewPage.messageHeaderUnreadIcon.waitForAbsence()
        }
    }
    
    public func isMessageOpened() -> Bool {
        self.messageViewPage.subjectLabel.exists
    }

    public func closeMessage() throws {
        try XCTContext.runActivity(named: "Closing message") { _ in
            try self.messageViewPage.backButton.tapCarefully()
        }
    }

    public func getOpenedMessage() throws -> FullMessageView {
        try XCTContext.runActivity(named: "Getting opened message") { _ in
            self.keepDetailsOpen = true
            try self.expandDetailsIfNeed()
            let message = FullMessage(
                Message(self.messageViewPage.messageHeaderFromLabel.label,
                        self.messageViewPage.subjectLabel.value as! String,
                        Int64(1), // TODO: parse date correctly
                        self.messageViewPage.messageBody.descendants(matching: .staticText).firstMatch.label,
                        self.getMessageCounter(),
                        !self.messageViewPage.messageHeaderUnreadIcon.exists,
                        try self.checkIfImportant(),
                        YSArray<AttachmentView>()
                ),
                try self.getTo(),
                self.messageViewPage.messageBody.getFullText(),
                TranslatorLanguageName.english,
                YSMap<LanguageName, String>()
            )
            self.keepDetailsOpen = false
            try self.collapseDetailsIfNeed()
            return message
        }
    }

    public func getLabels() throws -> YSSet<String> {
        try XCTContext.runActivity(named: "Getting labels for opened message") { _ in
            try self.expandDetailsIfNeed()
            let labelNames = self.messageViewPage.messageHeaderDetailsLabels.allElementsBoundByAccessibilityElement.map { element in
                element.getFullText()
            }
            try collapseDetailsIfNeed()
            return YSSet(labelNames)
        }
    }

    public func checkIfSpam() throws -> Bool {
        try XCTContext.runActivity(named: "Checking if opened message is in Spam") { _ in
            try self.expandDetailsIfNeed()
            if !self.messageViewPage.messageHeaderDetailsFolder.exists {
                return false
            }
            let isSpam = self.messageViewPage.messageHeaderDetailsFolder.getFullText() == DefaultFolderName.spam
            try self.collapseDetailsIfNeed()
            return isSpam
        }
    }

    public func checkIfRead() throws -> Bool {
        XCTContext.runActivity(named: "Checking if opened message is read") { _ in
            return !self.messageViewPage.messageHeaderUnreadIcon.exists
        }
    }

    public func checkIfImportant() throws -> Bool {
        try XCTContext.runActivity(named: "Checking if opened message is important") { _ in
            return try self.getLabels().contains("Important")
        }
    }

    public func deleteLabelsFromHeader(_ labels: YSArray<LabelName>) {
        // TODO: добавить айдентифаер для крестика у меток
        XCTContext.runActivity(named: "Deleting labels: \(labels.description) from opened message from header") { _ in
        }
        fatalError("deleteLabelsFromHeader() has not been implemented")
    }

    public func markAsUnimportantFromHeader() {
        // TODO: добавить айдентифаер для крестика у меток
        XCTContext.runActivity(named: "Marking as unimportant opened message from header") { _ in
        }
        fatalError("markAsUnimportantFromHeader() has not been implemented")
    }

    public func arrowDownClick() throws {
        try XCTContext.runActivity(named: "Tap on next message button") { _ in
            try self.messageViewPage.nextMessageButton.tapCarefully()
        }
    }

    public func arrowUpClick() throws {
        try XCTContext.runActivity(named: "Tap on previous message button") { _ in
            try self.messageViewPage.previousMessageButton.tapCarefully()
        }
    }
    
// TODO: parse date correctly
//    private func parseMessageDateToTimestamp() -> Int64 {
//        let dateString = self.messageViewPage.messageHeaderDateLabel.label
//        let dateFormatter = DateFormatter()
//        dateFormatter.locale = Locale(identifier: "en_US_POSIX") // set locale to reliable US_POSIX
//        dateFormatter.dateFormat = "MMM d, yyyy 'at' hh:mm a"
//        return doubleToInt64(dateFormatter.date(from: dateString)!.timeIntervalSince1970 * 1000)
//    }
    
    private func getMessageCounter() -> Int32! {
        let counter = Int32(self.messageViewPage.countsLabel.label.split(" ")[0])
        if counter == 1 {
            return nil
        }
        return counter
    }
    
    private func getTo() throws -> YSSet<String> {
        try self.expandDetailsIfNeed()
        let tos = self.messageViewPage.messageHeaderDetailsTo.allElementsBoundByAccessibilityElement.map { element in
            element.getFullText()
        }
        try self.collapseDetailsIfNeed()
        return YSSet(tos)
    }
    
    private func expandDetailsIfNeed() throws {
        if !self.messageViewPage.messageHeaderDetails.exists {
            try self.messageViewPage.messageHeaderDateLabel.tapCarefully()
        }
    }
    
    private func collapseDetailsIfNeed() throws {
        if self.messageViewPage.messageHeaderDetails.exists && !self.keepDetailsOpen {
            try self.messageViewPage.messageHeaderDateLabel.tapCarefully()
        }
    }
}
