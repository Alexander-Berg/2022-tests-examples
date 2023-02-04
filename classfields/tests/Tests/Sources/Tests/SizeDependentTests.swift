//
//  SizeDependentTests.swift
//  AutoRu
//
//  Created by Alexander Malnev on 04/04/2018.
//  Copyright © 2018 Auto.ru. All rights reserved.
//

import Foundation
import XCTest
@testable import AutoRuUtils

final class SizeDependentTests: BaseUnitTest {

    func testSizeDependentReturnsApproproateValuesForSize() {
        let sizeDependentString = SizeDependent("small")
        sizeDependentString[320] = "medium"
        sizeDependentString[414] = "large"
        sizeDependentString[760] = "extra large"

        XCTAssertEqual(sizeDependentString[0], "small")
        XCTAssertEqual(sizeDependentString[10], "small")
        XCTAssertEqual(sizeDependentString[319], "small")

        XCTAssertEqual(sizeDependentString[320], "medium")
        XCTAssertEqual(sizeDependentString[321], "medium")
        XCTAssertEqual(sizeDependentString[413], "medium")

        XCTAssertEqual(sizeDependentString[414], "large")
        XCTAssertEqual(sizeDependentString[444], "large")
        XCTAssertEqual(sizeDependentString[759], "large")

        XCTAssertEqual(sizeDependentString[760], "extra large")
        XCTAssertEqual(sizeDependentString[777], "extra large")
        XCTAssertEqual(sizeDependentString[999], "extra large")
        XCTAssertEqual(sizeDependentString[Int.max], "extra large")
    }
}
