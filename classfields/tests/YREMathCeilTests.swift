//
//  YREMathCeilTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Alexey Salangin on 9/25/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class YREMathCeilTests: XCTestCase {
    func testMathCeil() {
        let ins = [0.0, 0.001, 0.1, 0.5, 1.1, 1.23456, 1.009, 1.049, 1.235, 15.95]
        let outs1 = [0.0, 0.0, 0.1, 0.5, 1.1, 1.2, 1.0, 1.0, 1.2, 16.0]
        let outs2 = [0.0, 0.0, 0.1, 0.5, 1.1, 1.23, 1.01, 1.05, 1.24, 15.95]

        for (input, (out1, out2)) in zip(ins, zip(outs1, outs2)) {
            let result1 = YREMathCeil(input, 1)
            let result2 = YREMathCeil(input, 2)

            let out1_diff = abs(result1 - out1)
            let out2_diff = abs(result2 - out2)

            XCTAssertEqual(out1_diff, 0, accuracy: .ulpOfOne, "YREMathCeil(\(input) != \(out1)")
            XCTAssertEqual(out2_diff, 0, accuracy: .ulpOfOne, "YREMathCeil(\(input) != \(out2)")
        }
    }
}
