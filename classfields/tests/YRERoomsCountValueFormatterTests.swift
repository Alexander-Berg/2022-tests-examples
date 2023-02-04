//
//  YRERoomsCountValueFormatterTests.swift
//  YREAnalytics-Unit-Tests
//
//  Created by Ibrakhim Nikishin on 11/9/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import enum Typograf.SpecialSymbol

final class YRERoomsCountValueFormatterTests: XCTestCase {
    func testRoomsCountValues() throws {
        let dash = SpecialSymbol.ndash
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "1, 2, 3"), "1\(dash)3")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "1, 3"), "1, 3")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "1, 2, 7+"), "1, 2, 7+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "2, 3, 4, 7+"), "2\(dash)4, 7+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "2, 4, 5, 6, 7+"), "2, 4\(dash)6, 7+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "2, 3, 5, 6"), "2, 3, 5, 6")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "2, 3, 4, 5, 6"), "2\(dash)6")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия, 1, 2, 3"), "Студия, 1\(dash)3")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия, 1, 2, 3, 5"), "Студия, 1\(dash)3, 5")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия, 1, 2, 3, 4+"), "Студия, 1\(dash)3, 4+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия, 4+"), "Студия, 4+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия, 1, 2"), "Студия, 1, 2")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "Студия"), "Студия")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "4+"), "4+")
        XCTAssertEqual(YRERoomsCountValueFormatter.getModifiedString(for: "7+"), "7+")
    }
}
