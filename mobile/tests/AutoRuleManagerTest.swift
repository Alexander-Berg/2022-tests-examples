//
//  AutoRuleManagerTest.swift
//  Filters-Unit-Tests
//
//  Created by Timur Turaev on 15.04.2022.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

internal final class AutoRuleManagerTest: XCTestCase {
    private var autoRuleManager: AutoRuleManager!
    private var storage: TestStorage!

    private var showsCounter = 0
    private var modelToShowPromo: AutoRuleModel?

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.storage = TestStorage()
        self.autoRuleManager = AutoRuleManager(promoShowsStorage: self.storage)
        self.autoRuleManager.delegate = self
    }

    func testFrequentEvents() throws {
        self.storage.lastDate[123] = Date().newDate(diff: 12, granularity: .day)

        let eventModel = AutoRuleModel.markRead(from: "email1")
        let events = (1...8).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 0)
        XCTAssertNil(self.modelToShowPromo)
        XCTAssertTrue(self.storage.uid.isEmpty)
        XCTAssertTrue(self.storage.promoKind.isEmpty)
    }

    func testNonUniqueMidsInEvents() throws {
        let eventModel = AutoRuleModel.markRead(from: "email1")
        let events = (1...7).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }
        self.autoRuleManager.processEvent(AutoRuleEvent( model: eventModel, mid: 7), forUid: 123)

        XCTAssertEqual(self.showsCounter, 0)
        XCTAssertNil(self.modelToShowPromo)
        XCTAssertTrue(self.storage.uid.isEmpty)
        XCTAssertTrue(self.storage.promoKind.isEmpty)
    }

    func testDifferentUIDsInEvents() throws {
        let eventModel = AutoRuleModel.markRead(from: "email1")
        let events = (1...7).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }
        self.autoRuleManager.processEvent(AutoRuleEvent(model: eventModel, mid: 8), forUid: 1234)

        XCTAssertEqual(self.showsCounter, 0)
        XCTAssertNil(self.modelToShowPromo)
        XCTAssertTrue(self.storage.uid.isEmpty)
        XCTAssertTrue(self.storage.promoKind.isEmpty)
    }

    func testAlreadyTriggeredCounter() throws {
        let eventModel = AutoRuleModel.markRead(from: "email1")
        let events = (1...100).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 1)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123])
        XCTAssertEqual(self.storage.promoKind[123], .markRead)

        // another UID
        (1...100)
            .map { AutoRuleEvent(model: eventModel, mid: $0) }
            .forEach {
                self.autoRuleManager.processEvent($0, forUid: 1234)
            }

        XCTAssertEqual(self.showsCounter, 2)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123, 1234])
        XCTAssertEqual(self.storage.promoKind[123], .markRead)
        XCTAssertEqual(self.storage.promoKind[1234], .markRead)
    }

    // MARK: - Counters' test

    func testProcessMoveToFolderEvent() throws {
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .moveToFolder(from: "email10", folderID: 10, folderName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .moveToFolder(from: "email10", folderID: 10, folderName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .moveToFolder(from: "email10", folderID: 10, folderName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .moveToFolder(from: "email10", folderID: 10, folderName: ""), mid: 100), forUid: 123)

        XCTAssertEqual(self.showsCounter, 0)
        XCTAssertNil(self.modelToShowPromo)
        XCTAssertEqual(self.storage.uid, [])
        XCTAssertNil(self.storage.promoKind[123])

        let eventModel = AutoRuleModel.moveToFolder(from: "email1", folderID: 1, folderName: "")
        let events = (1...2).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 1)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123])
        XCTAssertEqual(self.storage.promoKind[123], .moveToFolder)
    }

    func testProcessApplyLabelEvent() throws {
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .applyLabel(from: "email10", labelID: 10, labelName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .applyLabel(from: "email11", labelID: 10, labelName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .applyLabel(from: "email10", labelID: 11, labelName: ""), mid: 100), forUid: 123)
        self.autoRuleManager.processEvent(AutoRuleEvent(model: .applyLabel(from: "email11", labelID: 11, labelName: ""), mid: 100), forUid: 123)

        XCTAssertEqual(self.showsCounter, 0)
        XCTAssertNil(self.modelToShowPromo)
        XCTAssertEqual(self.storage.uid, [])
        XCTAssertNil(self.storage.promoKind[123])

        let eventModel = AutoRuleModel.applyLabel(from: "email1", labelID: 1, labelName: "Label1")
        let events = (1...2).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 1)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123])
        XCTAssertEqual(self.storage.promoKind[123], .applyLabel)
    }

    func testProcessDeleteEvent() throws {
        let eventModel = AutoRuleModel.delete(from: "email1")
        let events = (1...2).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 1)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123])
        XCTAssertEqual(self.storage.promoKind[123], .delete)
    }

    func testProcessMarkReadEvent() throws {
        let eventModel = AutoRuleModel.markRead(from: "email1")
        let events = (1...8).map { AutoRuleEvent(model: eventModel, mid: $0) }
        events.forEach { self.autoRuleManager.processEvent($0, forUid: 123) }

        XCTAssertEqual(self.showsCounter, 1)
        XCTAssertEqual(self.modelToShowPromo, eventModel)
        XCTAssertEqual(self.storage.uid, [123])
        XCTAssertEqual(self.storage.promoKind[123], .markRead)
    }
}

extension AutoRuleManagerTest: AutoRuleManagerDelegate {
    func autoRuleManager(_ autoRuleManager: AutoRuleManager, didRequestToShowPromoFromAutoRule model: AutoRuleModel) {
        XCTAssertTrue(self.autoRuleManager === autoRuleManager)
        self.showsCounter += 1
        self.modelToShowPromo = model
    }
}

private final class TestStorage: PromoShowsDateStoring {
    fileprivate var lastDate: [YOIDType: Date] = .empty
    fileprivate var uid: [YOIDType] = .empty
    fileprivate var promoKind: [YOIDType: PromoKind] = .empty

    func setPromoLastShowDate(_ newValue: Date, for uid: YOIDType, promoKind: PromoKind) {
        self.lastDate[uid] = newValue
        self.uid.append(uid)
        self.promoKind[uid] = promoKind
    }

    func promoLastShowDateFor(uid: YOIDType, promoKind: PromoKind) -> Date {
        return self.lastDate[uid] ?? .distantPast
    }

    func clear() {
    }
}
