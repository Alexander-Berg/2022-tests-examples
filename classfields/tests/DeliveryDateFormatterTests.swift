//
//  DeliveryDateFormatterTests.swift
//  YREFormatters-Unit-Tests
//
//  Created by Fedor Solovev on 19.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREFiltersModel

final class DeliveryDateFormatterTests: XCTestCase {
    func testValueStringWhenEmptyAndNonFinished() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: nil, quarter: nil, finished: false)

        // when
        let value = DeliveryDateFormatter.valueString(fromDeliveryDate: deliveryDate)

        // then
        XCTAssertNil(value, "Value string isn't nil")
    }

    func testValueStringWhenFinished() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: nil, quarter: nil, finished: true)

        // when
        let value = DeliveryDateFormatter.valueString(fromDeliveryDate: deliveryDate)

        // then
        XCTAssertEqual(value, "Сдан", "Value strings aren't equal")
    }

    func testValueStringWithQuarterAndYear() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: 2020, quarter: 1, finished: false)

        // when
        let value = DeliveryDateFormatter.valueString(fromDeliveryDate: deliveryDate)

        // then
        XCTAssertEqual(value, "до 1 квартала 2020", "Value strings aren't equal")
    }

    func testValueStringOnlyWithYear() {
        // given
        let deliveryDate = YREFilterDeliveryDate(year: 2020, quarter: nil, finished: false)

        // when
        let value = DeliveryDateFormatter.valueString(fromDeliveryDate: deliveryDate)

        // then
        XCTAssertEqual(value, "до 2020 года", "Value strings aren't equal")
    }
}
