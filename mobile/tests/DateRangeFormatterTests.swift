//
//  DateRangeFormatterTests.swift
//  YandexMapsTests
//
//  Created by Dmitry Trimonov on 19/03/2019.
//  Copyright © 2019 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapsUtils

class DateRangeFormatterTests: XCTestCase {
    var dateRangeFormatter: DateRangeFormatter!

    let locale = Locale(identifier: "ru_RU")
    let strings = DateRangeFormatter.Strings(today: "Сегодня", fromToDateFormat: "с %@ по %@")

    var calendar: Calendar {
        return locale.calendar
    }

    override func setUp() {
        super.setUp()

        dateRangeFormatter = DateRangeFormatter(locale: locale, strings: strings)
    }

    func testTodayReturnsTodayString() {
        XCTAssert(dateRangeFormatter.formattedToday() == strings.today)
    }

    func testFormattedSingleReturnsDateAndMonthString() {
        let singleDate = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 19, hour: 5))!
        let actual = dateRangeFormatter.formattedSingle(singleDate)
        XCTAssert(actual == "19 марта")
    }

    func testFormattedSingleReturnsDateAndMonthString_1() {
        let singleDate = calendar.date(from: DateComponents(year: 2_019, month: 4, day: 1, hour: 5))!
        let actual = dateRangeFormatter.formattedSingle(singleDate)
        XCTAssert(actual == "1 апреля")
    }

    func testFormattedRangeWhenFromIsAfterToThrows() {
        let from = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 24, hour: 5))!
        let to = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 3, hour: 5))!
        do {
            _ = try dateRangeFormatter.formattedRange(from: from, to: to)
            XCTFail()
        } catch DateRangeFormatter.DateRangeFormatterError.fromGoesAfterTo {
            XCTAssert(true)
        } catch {
            XCTFail()
        }
    }

    func testFormattedRangeWhenDatesAreInSameMonthReturnsMonthOnlyForRangeEndDate() {
        let from = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 19, hour: 5))!
        let to = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 28, hour: 5))!
        do {
            let actual = try dateRangeFormatter.formattedRange(from: from, to: to)
            XCTAssert(actual == "С 19 по 28 марта")
        } catch {
            XCTFail()
        }
    }

    func testFormattedRangeWhenDatesAreNotInSameMonthReturnsMonthForRangeStartAndEndDates() {
        let from = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 19, hour: 5))!
        let to = calendar.date(from: DateComponents(year: 2_019, month: 5, day: 22, hour: 5))!
        do {
            let actual = try dateRangeFormatter.formattedRange(from: from, to: to)
            XCTAssert(actual == "С 19 марта по 22 мая")
        } catch {
            XCTFail()
        }
    }

    func testFormattedDatesWhenDatesAreEmptyReturnsEmptyString() {
        let dates: [Date] = []
        let actual = dateRangeFormatter.formattedDates(dates: dates)
        XCTAssert(actual.isEmpty)
    }

    func testFormattedDatesWhenDatesIsSingleDateArrayFallbacksToFormattedSingle() {
        let dates: [Date] = [calendar.date(from: DateComponents(year: 2_017, month: 2, day: 5, hour: 8))!]
        let actual = dateRangeFormatter.formattedDates(dates: dates)
        XCTAssert(actual == "5 февраля")
    }

    func testFormattedDatesWhenAllDatesAreInSameMonthReturnsDatesWithMonthAtTheEnd() {
        let dates: [Date] = [
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 5, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 13, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 21, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 22, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 23, hour: 8))!
        ]
        let actual = dateRangeFormatter.formattedDates(dates: dates)
        XCTAssert(actual == "5, 13, 21, 22, 23 февраля")
    }

    func testFormattedDatesWhenAllDatesAreNotInSameMonthReturnsDatesWithMonthForEachDate() {
        let dates: [Date] = [
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 5, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 1, day: 13, hour: 8))!,
            calendar.date(from: DateComponents(year: 2_017, month: 2, day: 21, hour: 8))!
        ]
        let actual = dateRangeFormatter.formattedDates(dates: dates)
        XCTAssert(actual == "5 февраля, 13 января, 21 февраля")
    }
}
