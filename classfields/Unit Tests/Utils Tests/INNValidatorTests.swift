//
//  INNValidatorTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 23.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YRECoreUtils

final class INNValidatorTests: XCTestCase {
     func testINNValid() {
        XCTAssertTrue(self.validator.validate(text: "500100732259"))
     }

    func testINNWithZeros() {
       XCTAssertFalse(self.validator.validate(text: "000000000000"))
    }
    
    func testINNWrongControlNumbers() {
        XCTAssertFalse(self.validator.validate(text: "012345678912"))
    }
    
    func testINNTooShort() {
        XCTAssertFalse(self.validator.validate(text: ""))
        XCTAssertFalse(self.validator.validate(text: "7830002293"))
    }
    
    func testINNTooLong() {
        XCTAssertFalse(self.validator.validate(text: "5001007322590"))
    }
    
    func testINNNotOnlyDigits() {
        XCTAssertFalse(self.validator.validate(text: "w50010073225"))
        XCTAssertFalse(self.validator.validate(text: "500100w73225"))
        XCTAssertFalse(self.validator.validate(text: "50010073225w"))
    }
    
    private let validator = INNValidator()
}
