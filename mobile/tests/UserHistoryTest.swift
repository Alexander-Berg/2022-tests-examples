//
// Created by Timur Turaev on 01/11/2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import XCTest
import TestUtils
@testable import Utils

public final class UserHistoryTest: XCTestCase {
    public func testRegisteringEvents() {
        let storage = InMemoryUserDefaults()
        let kind: UserHistoryKind = .activity
        let logger = TestLogger()
        let userHistory = UserHistory(storage: storage, kind: kind, logger: logger)

        let referenceDate = Date(timeIntervalSinceReferenceDate: 500_000_000) // суббота, 5 ноября 2016 г., 3:53:20 Москва, стандартное время

        userHistory.registerEvent(at: referenceDate)
        XCTAssertEqual(events(storage: storage, kind: kind).count, 1)

        // date is same as above
        userHistory.registerEvent(at: Date(timeInterval: 0, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 10, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 15_000, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 55_000, since: referenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 1)

        // воскресенье, 6 ноября 2016 г., 3:30:00 Москва, стандартное время
        userHistory.registerEvent(at: Date(timeInterval: 85_000, since: referenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 2)
        userHistory.registerEvent(at: Date(timeInterval: 115_000, since: referenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 2)

        // пятница, 24 ноября 2017 г., 3:10:00 Москва, стандартное время
        userHistory.registerEvent(at: Date(timeInterval: 33_175_000, since: referenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 3)
    }

    public func testReconstructionFromStorage() {
        let storage = InMemoryUserDefaults()
        let kind: UserHistoryKind = .activity
        let logger = TestLogger()

        let firstReferenceDate = Date(timeIntervalSinceReferenceDate: 500_000_000) // суббота, 5 ноября 2016 г., 3:53:20 Москва, стандартное время
        let secondReferenceDate = Date(timeIntervalSinceReferenceDate: 600_000_000) // понедельник, 6 января 2020 г., 13:40:00 Москва, стандартное время
        storage.setObject([firstReferenceDate, secondReferenceDate], forKey: kind.storageKey)

        XCTAssertEqual(storage.memory.count, 1)
        XCTAssertEqual(events(storage: storage, kind: kind).count, 2)

        let userHistory = UserHistory(storage: storage, kind: kind, logger: logger)
        userHistory.registerEvent(at: firstReferenceDate)
        userHistory.registerEvent(at: Date(timeInterval: 55_000, since: firstReferenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 115_000, since: firstReferenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 33_175_000, since: firstReferenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 2)

        userHistory.registerEvent(at: secondReferenceDate)
        userHistory.registerEvent(at: Date(timeInterval: 0, since: secondReferenceDate))

        // вторник, 7 января 2020 г., 4:56:40 Москва, стандартное время
        userHistory.registerEvent(at: Date(timeInterval: 55_000, since: secondReferenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 3)

        // вторник, 7 января 2020 г., 21:36:40 Москва, стандартное время
        userHistory.registerEvent(at: Date(timeInterval: 115_000, since: secondReferenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 3)

        // воскресенье, 24 января 2021 г., 12:56:40 Москва, стандартное время
        userHistory.registerEvent(at: Date(timeInterval: 33_175_000, since: secondReferenceDate))
        XCTAssertEqual(events(storage: storage, kind: kind).count, 4)
    }

    public func testClear() {
        let storage = InMemoryUserDefaults()
        let kind: UserHistoryKind = .activity
        let logger = TestLogger()
        let userHistory = UserHistory(storage: storage, kind: kind, logger: logger)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 500_000_000) // суббота, 5 ноября 2016 г., 3:53:20 Москва, стандартное время
        userHistory.registerEvent(at: referenceDate)
        userHistory.registerEvent(at: referenceDate)
        userHistory.registerEvent(at: referenceDate)
        XCTAssertEqual(events(storage: storage, kind: kind).count, 1)

        userHistory.clear()
        XCTAssertEqual(events(storage: storage, kind: kind).count, 0)

        userHistory.registerEvent(at: referenceDate)
        userHistory.registerEvent(at: referenceDate)
        XCTAssertEqual(events(storage: storage, kind: kind).count, 1)
    }

    public func testClosedEvents() {
        let logger = TestLogger()
        let userHistory = UserHistory(storage: InMemoryUserDefaults(), kind: .crash, logger: logger)

        let referenceDate = Date(timeIntervalSinceReferenceDate: 946_673_990) // пятница, 31 декабря 1999 г., 23:59:50 Москва, стандартное время
        userHistory.registerEvent(at: referenceDate)
        userHistory.registerEvent(at: Date(timeInterval: 59, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 118, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 177, since: referenceDate))

        XCTAssertTrue(userHistory.allEventsAreCloseToEachOther(withLengthUpTo: 10, withDifferenceAtMost: 1, of: .minute))
    }

    public func testNotClosedEvents() {
        let logger = TestLogger()
        let userHistory = UserHistory(storage: InMemoryUserDefaults(), kind: .activity, logger: logger)

        let referenceDate = Date(timeIntervalSinceReferenceDate: 946_673_990) // пятница, 31 декабря 1999 г., 23:59:50 Москва, стандартное время
        userHistory.registerEvent(at: referenceDate)
        userHistory.registerEvent(at: Date(timeInterval: 61, since: referenceDate))
        userHistory.registerEvent(at: Date(timeInterval: 63, since: referenceDate))

        XCTAssertFalse(userHistory.allEventsAreCloseToEachOther(withLengthUpTo: 10, withDifferenceAtMost: 1, of: .minute))
    }

    public func testNotClosedEmptyEvents() {
        let logger = TestLogger()
        let userHistory = UserHistory(storage: InMemoryUserDefaults(), kind: .activity, logger: logger)

        XCTAssertFalse(userHistory.allEventsAreCloseToEachOther(withLengthUpTo: 10, withDifferenceAtMost: 1, of: .minute))
    }

    public func testClosedOneEvent() {
        let logger = TestLogger()
        let userHistory = UserHistory(storage: InMemoryUserDefaults(), kind: .activity, logger: logger)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 946_673_990)
        userHistory.registerEvent(at: referenceDate)
        XCTAssertTrue(userHistory.allEventsAreCloseToEachOther(withLengthUpTo: 10, withDifferenceAtMost: 1, of: .minute))
    }

    public func testWipeDueToBug18395() {
        let logger = TestLogger()
        let storage = InMemoryUserDefaults()
        var userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        storage.setBool(false, forKey: UserHistoryKind.activity.wipeStorage18395key)

        let startDate = Date(timeIntervalSinceReferenceDate: 500_000_000)
        (1...80).forEach {
            userHistory.registerEvent(at: startDate.newDate(diff: $0, granularity: .day))
        }

        userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        XCTAssertNil(userHistory.lastRegisteredDate)
        XCTAssertTrue(storage.bool(forKey: UserHistoryKind.activity.wipeStorage18395key))
    }

    public func testNoWipeIfThereAreFewNumberOfEventsDueToBug18395() {
        let logger = TestLogger()
        let storage = InMemoryUserDefaults()
        var userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        storage.setBool(false, forKey: UserHistoryKind.activity.wipeStorage18395key)

        let startDate = Date(timeIntervalSinceReferenceDate: 500_000_000)
        (1...20).forEach {
            userHistory.registerEvent(at: startDate.newDate(diff: $0, granularity: .day))
        }

        userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        XCTAssertEqual(userHistory.lastRegisteredDate, startDate.newDate(diff: 20, granularity: .day))
        XCTAssertEqual(events(storage: storage, kind: .activity).count, 20)
        XCTAssertTrue(storage.bool(forKey: UserHistoryKind.activity.wipeStorage18395key))
    }

    public func testNoWipeTwiceDueToBug18395() {
        let logger = TestLogger()
        let storage = InMemoryUserDefaults()
        let startDate = Date(timeIntervalSinceReferenceDate: 500_000_000)
        storage.setObject((1...80).map({ startDate.newDate(diff: $0, granularity: .day) }), forKey: UserHistoryKind.activity.storageKey)

        var userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        XCTAssertEqual(events(storage: storage, kind: .activity).count, 0)
        XCTAssertTrue(storage.bool(forKey: UserHistoryKind.activity.wipeStorage18395key))
        XCTAssertNil(userHistory.lastRegisteredDate)

        (90...200).forEach {
            userHistory.registerEvent(at: startDate.newDate(diff: $0, granularity: .day))
        }

        userHistory = UserHistory(storage: storage, kind: .activity, logger: logger)
        XCTAssertEqual(events(storage: storage, kind: .activity).count, 111)
        XCTAssertEqual(userHistory.lastRegisteredDate, startDate.newDate(diff: 200, granularity: .day))
    }

    private func events(storage: InMemoryUserDefaults, kind: UserHistoryKind) -> [Date] {
        return storage.memory[kind.storageKey] as! [Date]
    }
}
