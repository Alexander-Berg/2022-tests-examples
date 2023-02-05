//
//  CharsFormatterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 30.07.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class CharsFormatterTest: XCTestCase {
    private var charsFormatter: CharsFormatter! = CharsFormatter()

    func testInvalidChars() {
        ["кириллица", "кириллицаlatin"].forEach { login in
            XCTAssertFalse(self.charsFormatter.check(login))
        }
        Array("+=_)(*,^&%№!кириллица").forEach { symbol in
            XCTAssertFalse(self.charsFormatter.check("fail\(String(symbol))test"))
        }

        Array(".-0123456789latin").forEach { symbol in
            XCTAssertTrue(self.charsFormatter.check("normal\(String(symbol))test"))
        }
    }

    override func tearDown() {
        self.charsFormatter = nil

        super.tearDown()
    }
}
