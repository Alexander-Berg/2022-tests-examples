//
//  CurrentDateProviderTest.swift
//  TabbarTests
//
//  Created by Aleksey Makhutin on 26.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import Tabbar

internal final class CurrentDateProviderTest: XCTestCase {
    func testStringDayOfMounthContainsOneSymbol() {
        let internalDate = Date(timeIntervalSince1970: 0)
        for days in 0..<8 {
            var dayComponent = DateComponents()
            dayComponent.day = days
            let calendar = Calendar.current
            let testDate = calendar.date(byAdding: dayComponent, to: internalDate)
            let dateProvider = CurrentDateProvider(date: testDate)
            XCTAssertEqual(dateProvider.dayOfMonth.count, 1, "the day of mounth from 1 to 9 must contains one symbol")
        }
    }

    func testStringDayOfMounthContainsTwoSymbol() {
        let internalDate = Date(timeIntervalSince1970: 0)
        for days in 9..<30 {
            var dayComponent = DateComponents()
            dayComponent.day = days
            let calendar = Calendar.current
            let testDate = calendar.date(byAdding: dayComponent, to: internalDate)
            let dateProvider = CurrentDateProvider(date: testDate)
            XCTAssertEqual(dateProvider.dayOfMonth.count, 2, "the day of mounth from 10 to 31 must contains two symbol")
        }
    }
}
