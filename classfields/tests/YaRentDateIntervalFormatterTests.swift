//
//  YaRentDateIntervalFormatterTests.swift
//  YREFormatters-Unit-Tests
//
//  Created by Dmitry Barillo on 16.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
import YREFormatters

class YaRentDateIntervalFormatterTests: XCTestCase {
    func testTimeDistanceWithZeroTime() {
        let formatter = YaRentDateIntervalFormatter()
        let dateFormatter = Self.dateFormatter

        let differentYearDateInteraval = DateInterval(
            start: dateFormatter.date(from: "2021-11-15")!,
            end: dateFormatter.date(from: "2022-01-25")!
        )
        let differentMonthDateInteraval = DateInterval(
            start: dateFormatter.date(from: "2021-11-15")!,
            end: dateFormatter.date(from: "2021-12-25")!
        )
        let differentDayDateInteraval = DateInterval(
            start: dateFormatter.date(from: "2021-11-15")!,
            end: dateFormatter.date(from: "2021-11-25")!
        )
        let differentYearDateInteravalString = formatter.string(dateInterval: differentYearDateInteraval)
        let differentMonthDateInteravalString = formatter.string(dateInterval: differentMonthDateInteraval)
        let differentDayDateInteravalString = formatter.string(dateInterval: differentDayDateInteraval)

        let differentYearDateInteravalExpectedString = "15 нояб. – 25 янв.".yre_insertNBSPs()
        // swiftlint:disable:next identifier_name
        let differentMonthDateInteravalExpectedString = "15 нояб. – 25 дек.".yre_insertNBSPs()
        let differentDayInteravalExpectedString = "15 нояб. – 25 нояб.".yre_insertNBSPs()

        XCTAssertEqual(differentYearDateInteravalString, differentYearDateInteravalExpectedString)
        XCTAssertEqual(differentMonthDateInteravalString, differentMonthDateInteravalExpectedString)
        XCTAssertEqual(differentDayDateInteravalString, differentDayInteravalExpectedString)
    }

    private static let dateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        return dateFormatter
    }()
}
