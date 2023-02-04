//
//  DeliveryDateOptionsSerializerTests.swift
//  YREWeb-Unit-Tests
//
//  Created by Fedor Solovev on 19.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREWeb
import YREFiltersModel

final class DeliveryDateOptionsSerializerTests: XCTestCase {
    func testSerializedValueWithDeliveryDateIsEmpty() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: nil, quarter: nil, finished: false)

        // when
        let serializedValue = DeliveryDateOptionsSerializer.serializedValue(for: deliveryDate)

        // then
        XCTAssertNil(serializedValue, "Serialized value isn't nil")
    }

    func testSerializedValueWithDeliveryDateIsFinished() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: nil, quarter: nil, finished: true)

        // when
        let serializedValue = DeliveryDateOptionsSerializer.serializedValue(for: deliveryDate)

        // then
        XCTAssertNotNil(serializedValue, "Serialized value is nil")
        XCTAssertEqual(serializedValue, "FINISHED", "Serialized value isn't nil")
    }

    func testSerializedValueWithDeliveryDateIsNotFinished() {
        // given
        let year = 2020
        let quarter = 4
        let deliveryDate = YREFilterDeliveryDate(year: NSNumber(value: year),
                                                 quarter: NSNumber(value: quarter),
                                                 finished: false)
        let expectedSerializedValue = "\(quarter)_\(year)"

        // when
        let serializedValue = DeliveryDateOptionsSerializer.serializedValue(for: deliveryDate)

        // then
        XCTAssertNotNil(serializedValue, "Serialized value is nil")
        XCTAssertEqual(serializedValue, expectedSerializedValue, "Serialized value isn't nil")
    }

    func testSerializedValueWithDeliveryDatesAreEmpty() {
        // given
        let deliveryDates: [YREFilterDeliveryDate] = []

        // when
        let serializedValues = DeliveryDateOptionsSerializer.serializedValue(forValidOptions: deliveryDates)

        // then
        XCTAssertEqual(serializedValues.isEmpty, true, "Serialized values is empty")
    }

    func testSerializedValueWithDeliveryDates() {
        // given
        let year = 2020
        let quarters: [Int] = Array(1...4)
        let deliveryDatesAndSerializedValues = quarters.map { quarter in
            (YREFilterDeliveryDate(year: NSNumber(value: year),
                                   quarter: NSNumber(value: quarter),
                                   finished: false),
             "\(quarter)_\(year)")
        }
        let deliveryDates = deliveryDatesAndSerializedValues.map { $0.0 }

        // when
        let serializedValues = DeliveryDateOptionsSerializer.serializedValue(forValidOptions: deliveryDates)

        // then
        XCTAssertEqual(serializedValues.isEmpty, false, "Serialized values is empty")
        deliveryDatesAndSerializedValues.forEach { deliveryDate, expectedSerializedValue in
            let serializedValue = serializedValues[deliveryDate]
            XCTAssertEqual(serializedValue, expectedSerializedValue, "Serialized values \(expectedSerializedValue) aren't equal")
        }
    }
}
