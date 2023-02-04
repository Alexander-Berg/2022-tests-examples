//
//  YREPhoneFormatterTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 13.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREFormatters

final class YREPhoneFormatterTests: XCTestCase {
    let sanitizedPhone = "+79876543210"
    let formattedPhone = "+7 987 654-32-10"

    func testSanitizedPhone() {
        var sanitized: String?

        // Remove all characters instead of numbers and "+" sign
        sanitized = PhoneFormatter.sanitizedPhone(self.formattedPhone)
        XCTAssertNotNil(sanitized)
        XCTAssertEqual(sanitized, self.sanitizedPhone)

        sanitized = PhoneFormatter.sanitizedPhone("+7(132)432-1-234@abc")
        XCTAssertNotNil(sanitized)
        XCTAssertEqual(sanitized, "+71324321234")

        let emptyString = ""
        sanitized = PhoneFormatter.sanitizedPhone(emptyString)
        XCTAssertNotNil(sanitized)
        XCTAssertEqual(sanitized, emptyString)
    }

    func testFormattedPhone() {
        var formatted: String?

        // Use the mask "+7 XXX XXX-XX-XX"
        formatted = PhoneFormatter.formattedPhone(self.sanitizedPhone)
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, self.formattedPhone)

        // Replace first "8" with "+7 " and add "-" dividers
        formatted = PhoneFormatter.formattedPhone("89876543210")
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, self.formattedPhone)

        // Add "+7 " at the start and "-" dividers
        formatted = PhoneFormatter.formattedPhone("9876543210")
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, self.formattedPhone)

        // Add "+7 " at the start
        formatted = PhoneFormatter.formattedPhone("12345")
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, "+7 12345")

        // Pass through without formatting
        let longNumberString = "987654321987654321"
        formatted = PhoneFormatter.formattedPhone(longNumberString)
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, longNumberString)

        // Pass through without formatting
        let emptyString = ""
        formatted = PhoneFormatter.formattedPhone(emptyString)
        XCTAssertNotNil(formatted)
        XCTAssertEqual(formatted, emptyString)
    }
}
