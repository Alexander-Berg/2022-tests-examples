//
//  AnyOfferNameFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Arkady Smirnov on 10/9/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters

class AnyOfferNameFormatterTests: XCTestCase {
    typealias Pair = (inputString: String, expectation: String)
    
    func testCapitalizedName() {
        let pairs: [Pair] = [
            ("test", "Test"),
            ("test one", "Test one"),
            ("test кп one", "Test кп one"),
            ("test жк one", "Test жк one"),
            ("test мфк one", "Test мфк one"),
            ("жк test", "ЖК test"),
            ("Жк test", "ЖК test"),
            ("жК test", "ЖК test"),
            ("ЖК test", "ЖК test"),
            ("кп test", "КП test"),
            ("кП test", "КП test"),
            ("Кп test", "КП test"),
            ("КП test", "КП test"),
            ("мфк test", "МФК test"),
            ("Мфк test", "МФК test"),
            ("МФК test", "МФК test")
        ]
        for pair in pairs {
            let capitalized = AnyOfferNameFormatter.capitalizedName(from: pair.inputString)
            XCTAssertEqual(capitalized, pair.expectation)
        }
    }
}
