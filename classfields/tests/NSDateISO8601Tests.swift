//
//  NSDateISO8601Tests.swift
//  YREPackagesTests
//
//  Created by Dmitry Barillo on 14/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREWeb

// NOTE: We compare 'timeIntervalSince1970' in tests
// because DateFormatter got inaccuracy (conversion from double to float and vice versa)
// while parsing milliseconds from string. Then comparing two equals date they aren't equal. =(

final class NSDateISO8601Tests: XCTestCase {

    // MARK: - Simple ISO8601DateFormatter

    func testSimpleISO8601DateFormatter() {
        let rawDates = [
            "2018-12-10T17:29:50Z",
            "2018/12/10T17:29:50Z"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 0,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 0
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    // MARK: - Simple ISO8601DateFormatter with time zone

    func testSimpleWithTimeZoneISO8601DateFormatterWithZeroOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50-00:00",
            "2018/12/10T17:29:50-00:00",
            "2018-12-10T17:29:50-0000",
            "2018/12/10T17:29:50-0000",
            "2018-12-10T17:29:50-00",
            "2018/12/10T17:29:50-00"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 0,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 0
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    func testSimpleWithTimeZoneISO8601DateFormatterWithNegativeOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50-06:00",
            "2018/12/10T17:29:50-06:00",
            "2018-12-10T17:29:50-0600",
            "2018/12/10T17:29:50-0600",
            "2018-12-10T17:29:50-06",
            "2018/12/10T17:29:50-06"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: -6 * 3600,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 0
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    func testSimpleWithTimeZoneISO8601DateFormatterWithPositiveOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50+06:00",
            "2018/12/10T17:29:50+06:00",
            "2018-12-10T17:29:50+0600",
            "2018/12/10T17:29:50+0600",
            "2018-12-10T17:29:50+06",
            "2018/12/10T17:29:50+06"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 6 * 3600,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 0
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    // MARK: - Extended ISO8601DateFormatter

    func testExtendedISO8601DateFormatter() {
        let rawDates = [
            "2018-12-10T17:29:50.999Z",
            "2018/12/10T17:29:50.999Z"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 0,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 999_000_000
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    // MARK: - Extended ISO8601DateFormatter with time zone

    func testExtendedWithTimeZoneISO8601DateFormatterWithZeroOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50.999-00:00",
            "2018/12/10T17:29:50.999-00:00",
            "2018-12-10T17:29:50.999-0000",
            "2018/12/10T17:29:50.999-0000",
            "2018-12-10T17:29:50.999-00",
            "2018/12/10T17:29:50.999-00"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 0,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 999_000_000
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    func testExtendedWithTimeZoneISO8601DateFormatterWithNegativeOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50.999-06:00",
            "2018/12/10T17:29:50.999-06:00",
            "2018-12-10T17:29:50.999-0600",
            "2018/12/10T17:29:50.999-0600",
            "2018-12-10T17:29:50.999-06",
            "2018/12/10T17:29:50.999-06"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: -6 * 3600,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 999_000_000
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    func testExtendedWithTimeZoneISO8601DateFormatterWithPositiveOffsetTimeZone() {
        let rawDates = [
            "2018-12-10T17:29:50.999+06:00",
            "2018/12/10T17:29:50.999+06:00",
            "2018-12-10T17:29:50.999+0600",
            "2018/12/10T17:29:50.999+0600",
            "2018-12-10T17:29:50.999+06",
            "2018/12/10T17:29:50.999+06"
        ]

        let expectedDate = Self.date(
            secondsFromGMT: 6 * 3600,
            year: 2018,
            month: 12,
            day: 10,
            hour: 17,
            minute: 29,
            second: 50,
            nanosecond: 999_000_000
        )

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertEqual(date?.timeIntervalSince1970, expectedDate?.timeIntervalSince1970)
        }
    }

    // MARK: - Invalid format

    func testISO8601DateFormatterWithInvalidDateFormat() {
        let rawDates = [
            "20171009T181514Z",
            "20171009T181514+06",
            "20171009T181514+0600",
            "20171009T181514.999",
            "20171009T181514.999+06",
            "20171009T181514.999+0600",
            "2018-12-10T17:29:50,999Z",
            "2018/12/10T17:29:50,999Z",
            "2018-12-10 17:29:50Z",
            "2018-12-10 172950+06",
            "2018-12-10 172950+0600",
            "2018-12-10T17:29:50",
            "2018/12/10T17:29:50"
        ]

        for rawDate in rawDates {
            let date = NSDate.yre_date(fromISO8601FormatString: rawDate)
            XCTAssertNil(date)
        }
    }
}


extension NSDateISO8601Tests {
    private static func date(
        secondsFromGMT: Int,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        nanosecond: Int
    ) -> Date? {
        let calendar = Calendar(identifier: .gregorian)
        let dateComponents = DateComponents(
            calendar: calendar,
            timeZone: TimeZone(secondsFromGMT: secondsFromGMT),
            era: nil,
            year: year,
            month: month,
            day: day,
            hour: hour,
            minute: minute,
            second: second,
            nanosecond: nanosecond,
            weekday: nil,
            weekdayOrdinal: nil,
            quarter: nil,
            weekOfMonth: nil,
            weekOfYear: nil,
            yearForWeekOfYear: nil
        )
        let result = calendar.date(from: dateComponents)
        return result
    }
}
