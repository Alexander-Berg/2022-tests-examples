//
//  EmailsComparatorTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 25.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import Utils

internal final class EmailsComparatorTest: XCTestCase {
    func testCompareOtherEmails() {
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "t-e.s-t@other.ru", secondEmail: "t-e.s-t@other.ru"))
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "tESt@other.ru", secondEmail: "test@other.ru"))

        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "t-est@other.ru", secondEmail: "t.est@other.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "tESt@other.ru", secondEmail: "test@other.com"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "tESt@other.ru", secondEmail: "test@otherr.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "test@other.ru", secondEmail: "test1@other.ru"))
    }

    func testCompareNilsAndInvalidEmails() {
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: nil, secondEmail: "t-e.s-t@other.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "a@a.ru", secondEmail: nil))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: nil, secondEmail: nil))

        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "string@a@a", secondEmail: "string@a@b"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "string", secondEmail: "string@a@b"))
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "string@a@a", secondEmail: "string@a@a"))
    }

    func testCompareYandexEmails() {
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "t-e.s-t@yandex.ru", secondEmail: "t.e-s.t@yandex.ru"))
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "t-e.s-t@yandex.ru", secondEmail: "t-e-s.t@ya.ru"))
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "t-e.s-t@yandex.ru", secondEmail: "t.E-S.t@ya.ru"))
        XCTAssertTrue(EmailsComparator.isEqualOrAliases(firstEmail: "test@ya.ru", secondEmail: "test@ya.ru"))

        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "t-e.s-t@yandex.ru", secondEmail: "t.E-S.t@other.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "test@yandex.ru", secondEmail: "test@other.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "t..est@yandex.ru", secondEmail: "t-est@other.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "test@yandex.ru", secondEmail: "test@yandex2.ru"))
        XCTAssertFalse(EmailsComparator.isEqualOrAliases(firstEmail: "test@ya.ru", secondEmail: "test1@ya.ru"))
    }
}
