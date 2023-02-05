//
// Created by Timur Turaev on 06/11/2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import XCTest
import TestUtils
@testable import Utils

public final class UserHistoryManagerTest: XCTestCase {
    public func testHandlingNotification() {
        let manager = UserHistoryManager.shared
        let logger = TestLogger()
        manager.initialize(storage: InMemoryUserDefaults(), logger: logger)
        XCTAssertNil(manager.acquireUserHistory(kind: .activity).lastRegisteredDate)
        XCTAssertNil(manager.acquireUserHistory(kind: .crash).lastRegisteredDate)

        NotificationCenter.default.post(name: .UserHistoryEvent, object: UserHistoryEvent(kind: .activity, date: Date(timeIntervalSinceReferenceDate: 100)))
        NotificationCenter.default.post(name: .UserHistoryEvent, object: UserHistoryEvent(kind: .crash, date: Date(timeIntervalSinceReferenceDate: 200)))

        XCTAssertEqual(manager.acquireUserHistory(kind: .activity).lastRegisteredDate, Date(timeIntervalSinceReferenceDate: 100))
        XCTAssertEqual(manager.acquireUserHistory(kind: .crash).lastRegisteredDate, Date(timeIntervalSinceReferenceDate: 200))

        NotificationCenter.default.post(name: .UserHistoryEvent, object: UserHistoryEvent(kind: .activity, date: Date(timeIntervalSinceReferenceDate: 3_000_000)))
        NotificationCenter.default.post(name: .UserHistoryEvent, object: UserHistoryEvent(kind: .activity, date: Date(timeIntervalSinceReferenceDate: 7_000_000)))
        NotificationCenter.default.post(name: .UserHistoryEvent, object: UserHistoryEvent(kind: .crash, date: Date(timeIntervalSinceReferenceDate: 4_000_000)))

        XCTAssertEqual(manager.acquireUserHistory(kind: .activity).lastRegisteredDate, Date(timeIntervalSinceReferenceDate: 7_000_000))
        XCTAssertEqual(manager.acquireUserHistory(kind: .crash).lastRegisteredDate, Date(timeIntervalSinceReferenceDate: 4_000_000))
    }

    public func testRegistrationUserHistoriesFromNonEmptyStorage() {
        let storage = InMemoryUserDefaults()
        let logger = TestLogger()
        let firstReferenceDate = Date(timeIntervalSinceReferenceDate: 500_000_000) // суббота, 5 ноября 2016 г., 3:53:20 Москва, стандартное время
        let secondReferenceDate = Date(timeIntervalSinceReferenceDate: 600_000_000) // понедельник, 6 января 2020 г., 13:40:00 Москва, стандартное время
        storage.setObject([firstReferenceDate, secondReferenceDate], forKey: UserHistoryKind.activity.storageKey)

        let manager = UserHistoryManager.shared
        manager.initialize(storage: storage, logger: logger)

        XCTAssertEqual(manager.acquireUserHistory(kind: .activity).lastRegisteredDate, secondReferenceDate)
        XCTAssertNil(manager.acquireUserHistory(kind: .crash).lastRegisteredDate)
    }

    public func testClearing() {
        let storage = InMemoryUserDefaults()
        let logger = TestLogger()
        let firstReferenceDate = Date(timeIntervalSinceReferenceDate: 500_000_000) // суббота, 5 ноября 2016 г., 3:53:20 Москва, стандартное время
        let secondReferenceDate = Date(timeIntervalSinceReferenceDate: 600_000_000) // понедельник, 6 января 2020 г., 13:40:00 Москва, стандартное время
        storage.setObject([firstReferenceDate, secondReferenceDate], forKey: UserHistoryKind.activity.storageKey)
        storage.setObject([firstReferenceDate], forKey: UserHistoryKind.crash.storageKey)

        let manager = UserHistoryManager.shared
        manager.initialize(storage: storage, logger: logger)
        manager.clear()

        XCTAssertTrue((storage.object(forKey: UserHistoryKind.activity.storageKey) as! [Date]).isEmpty)
        XCTAssertTrue((storage.object(forKey: UserHistoryKind.crash.storageKey) as! [Date]).isEmpty)
    }
}
