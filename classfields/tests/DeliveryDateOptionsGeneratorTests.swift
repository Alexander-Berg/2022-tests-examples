//
//  DeliveryDateOptionsGeneratorTests.swift
//  YREFormatters-Unit-Tests
//
//  Created by Fedor Solovev on 19.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREFiltersModel

final class DeliveryDateOptionsGeneratorTests: XCTestCase {
    func testValidOptionsForDate() {
        // given
        var dateComponents = DateComponents()
        dateComponents.year = 2020
        dateComponents.month = 1
        let date = Calendar.current.date(from: dateComponents) ?? Date()

        let expectedFinishedValue = YREFilterDeliveryDate(year: nil, quarter: nil, finished: true)
        let expectedDeliveryDates = [
            YREFilterDeliveryDate(year: 2020, quarter: 1, finished: false),
            YREFilterDeliveryDate(year: 2020, quarter: 2, finished: false),
            YREFilterDeliveryDate(year: 2020, quarter: 3, finished: false),
            YREFilterDeliveryDate(year: 2020, quarter: 4, finished: false),
            YREFilterDeliveryDate(year: 2021, quarter: 1, finished: false),
            YREFilterDeliveryDate(year: 2021, quarter: 2, finished: false),
            YREFilterDeliveryDate(year: 2021, quarter: 3, finished: false),
            YREFilterDeliveryDate(year: 2021, quarter: 4, finished: false)
        ]

        // when
        var validOptions = DeliveryDateOptionsGenerator.validOptions(forDate: date)

        // then
        XCTAssertEqual(validOptions.count, 9, "Count of validOptions isn't equal")
        let finishedValue = validOptions.removeFirst()
        XCTAssertEqual(finishedValue, expectedFinishedValue, "Finished values aren't equal")
        XCTAssertEqual(validOptions.count, expectedDeliveryDates.count, "Count of deliveryDates isn't equal")
        for (index, expectedDeliveryDate) in expectedDeliveryDates.enumerated() {
            let validOption = validOptions[index]
            XCTAssertEqual(validOption, expectedDeliveryDate, "Delivery date \(index) isn't equal")
        }
    }

    func testOptionsToDisplayedStringsWithValidOptionsAreEmpty() {
        // given
        let validOptions: [YREFilterDeliveryDate] = []

        // when
        let displayedStrings = DeliveryDateOptionsGenerator.optionsToDisplayedStrings(forValidOptions: validOptions)

        // then
        XCTAssertEqual(displayedStrings.isEmpty, true, "Displayed strings aren't empty")
    }

    func testOptionsToDisplayedStringWithFinished() {
        // given
        let finished = YREFilterDeliveryDate(year: nil,
                                             quarter: nil,
                                             finished: true)
        let validOptions = [finished]

        // when
        let displayedStrings = DeliveryDateOptionsGenerator.optionsToDisplayedStrings(forValidOptions: validOptions)

        // then
        XCTAssertEqual(displayedStrings.count, 1, "Displayed strings count isn't equal")
        XCTAssertEqual(displayedStrings[finished], "Сдан", "Finished string isn't equal")
    }

    func testOptionsToDisplayedStrings() {
        // given
        let year = 2020
        let quarters: [Int] = Array(1...4)
        let deliveryDatesAndDisplayedStrings = quarters.map { quarter in
            (YREFilterDeliveryDate(year: NSNumber(value: year),
                                   quarter: NSNumber(value: quarter),
                                   finished: false),
             "до \(quarter) квартала \(year)")
        }
        let validOptions: [YREFilterDeliveryDate] = deliveryDatesAndDisplayedStrings.map { $0.0 }

        // when
        let displayedStrings = DeliveryDateOptionsGenerator.optionsToDisplayedStrings(forValidOptions: validOptions)

        // then
        XCTAssertEqual(displayedStrings.count, 4, "Displayed strings count isn't equal")
        deliveryDatesAndDisplayedStrings.forEach { deliveryDate, expectedDisplayedString in
            let displayedString = displayedStrings[deliveryDate]
            XCTAssertEqual(displayedString, expectedDisplayedString, "Delivery dates \(expectedDisplayedString) aren't equal")
        }
    }
}
