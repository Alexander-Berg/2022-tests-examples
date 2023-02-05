//
//  SemanticVersionTest.swift
//  StylerTests
//
//  Created by Nikita Ermolenko on 21.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import Utils

internal final class SemanticVersionTest: XCTestCase {
    func testShouldParseProperly() {
        XCTAssertEqual(try? SemVer(string: "1.0.0"), SemVer(major: 1))
        XCTAssertEqual(try? SemVer(string: "30.20.10"), SemVer(major: 30, minor: 20, patch: 10))

        XCTAssertEqual(try? SemVer(string: "1.0.0-alpha"), SemVer(major: 1, minor: 0, patch: 0, prerelease: [.identifier("alpha")]))
        XCTAssertEqual(try? SemVer(string: "1.0.0-al-ph4.1.test"), SemVer(major: 1, minor: 0, patch: 0, prerelease: [.identifier("al-ph4"), .number(1), .identifier("test")]))

        XCTAssertEqual(try? SemVer(string: "1.0.0+beta"), SemVer(major: 1, minor: 0, patch: 0, metadata: [.identifier("beta")]))
        XCTAssertEqual(try? SemVer(string: "1.0.0+be-t4.1.02.test"),
                       SemVer(major: 1, minor: 0, patch: 0, metadata: [.identifier("be-t4"), .number(1), .number(2), .identifier("test")]))

        XCTAssertEqual(try? SemVer(string: "1.0.0-al-ph4.1+be-t4.1.0d2"),
                       SemVer(major: 1,
                              minor: 0,
                              patch: 0,
                              prerelease: [.identifier("al-ph4"), .number(1)],
                              metadata: [.identifier("be-t4"), .number(1), .identifier("0d2")]))

        XCTAssertThrowsError(try SemVer(string: "1.0")) // all three components must be present
        XCTAssertThrowsError(try SemVer(string: "1")) // all three components must be present
        XCTAssertThrowsError(try SemVer(string: "1.0.0-alpha.1.")) // cannot end with dot
        XCTAssertThrowsError(try SemVer(string: "1.0.0-.1")) // preprelease identifiers must not be empty
        XCTAssertThrowsError(try SemVer(string: "1.0.0-01")) // prerelease no leading zeroes allowed
        XCTAssertThrowsError(try SemVer(string: "1.0.0+.1")) // metadata identifiers must not be empty
    }

    func testShouldSupportProperOrdering() {
        XCTAssertEqual(try! SemVer(string: "1.0.0-alpha.1+meta.2"), try! SemVer(string: "1.0.0-alpha.1+meta.2"))
        XCTAssertNotEqual(try! SemVer(string: "1.0.0-alpha.1+meta.2"), try! SemVer(string: "1.0.0-alpha.1"))
        XCTAssertNotEqual(try! SemVer(string: "1.0.0-alpha.1+meta.2"), try! SemVer(string: "1.0.0+meta.2"))
        XCTAssertNotEqual(try! SemVer(string: "1.0.0-alpha.1+meta.2"), try! SemVer(string: "1.0.1-alpha.1+meta.2"))

        XCTAssertLessThan(try! SemVer(string: "0.0.1"), try! SemVer(string: "0.0.2"))
        XCTAssertLessThan(try! SemVer(string: "0.0.1"), try! SemVer(string: "1.0.0"))
        XCTAssertLessThan(try! SemVer(string: "0.0.1"), try! SemVer(string: "0.1.0"))
        XCTAssertLessThan(try! SemVer(string: "0.1.0"), try! SemVer(string: "0.2.0"))
        XCTAssertLessThan(try! SemVer(string: "0.1.0"), try! SemVer(string: "1.0.0"))
        XCTAssertLessThan(try! SemVer(string: "1.0.0"), try! SemVer(string: "2.0.0"))

        XCTAssertTrue(try! SemVer(string: "1.2.3") < (try! SemVer(string: "1.4.2")))
        XCTAssertFalse(try! SemVer(string: "1.4.2") < (try! SemVer(string: "1.2.3")))

        XCTAssertLessThan(try! SemVer(string: "1.0.0"), try! SemVer(string: "1.0.0-alpha"))
        XCTAssertLessThan(try! SemVer(string: "1.0.0-alpha"), try! SemVer(string: "1.0.0-beta"))
        XCTAssertLessThan(try! SemVer(string: "1.0.0-0-alpha"), try! SemVer(string: "1.0.0-1-alpha"))
        XCTAssertLessThan(try! SemVer(string: "1.0.0-0-alpha"), try! SemVer(string: "1.0.0-0-beta"))

        XCTAssertTrue(try! SemVer(string: "1.0.0-alpha+beta").hasSamePrecedence(as: "1.0.0-alpha+gamma"))
        XCTAssertFalse(try! SemVer(string: "1.0.0-alpha+beta").hasSamePrecedence(as: "1.0.0-gamma+beta"))

        XCTAssertTrue(try! SemVer(string: "1.0.0-alpha+beta").hasSameBaseVersion(as: "1.0.0-gamma+delta"))
        XCTAssertFalse(try! SemVer(string: "1.0.1-alpha+beta").hasSameBaseVersion(as: "1.0.0-alpha+beta"))

        XCTAssertTrue(try! SemVer(string: "1.1.0-alpha+beta").hasAdditiveChanges(since: "1.0.0-alpha+beta"))
        XCTAssertTrue(try! SemVer(string: "1.1.0-alpha+beta").hasAdditiveChanges(since: "0.1.0-alpha+beta"))
        XCTAssertFalse(try! SemVer(string: "1.0.1-alpha+beta").hasAdditiveChanges(since: "1.0.0-alpha+beta"))

        XCTAssertTrue(try! SemVer(string: "1.1.1-alpha+beta").hasPatches(since: "1.1.0-alpha+beta"))
        XCTAssertTrue(try! SemVer(string: "1.1.1-alpha+beta").hasPatches(since: "1.0.1-alpha+beta"))
        XCTAssertTrue(try! SemVer(string: "1.1.1-alpha+beta").hasPatches(since: "0.1.1-alpha+beta"))
        XCTAssertFalse(try! SemVer(string: "1.0.0-alpha+beta").hasPatches(since: "1.0.0-beta+beta"))

        XCTAssertTrue(try! SemVer(string: "2.0.0-alpha+beta").hasBreakingChanges(since: "1.0.0-alpha+beta"))
        XCTAssertFalse(try! SemVer(string: "1.0.1-alpha+beta").hasBreakingChanges(since: "1.0.0-alpha+beta"))
        XCTAssertFalse(try! SemVer(string: "1.1.1-alpha+beta").hasBreakingChanges(since: "1.0.0-alpha+beta"))

        XCTAssertTrue(try! SemVer(string: "0.0.1-alpha+beta").representsInitialDevelopment)
        XCTAssertFalse(try! SemVer(string: "1.0.1-alpha+beta").representsInitialDevelopment)
    }

    func testIncreasingSemver() {
        XCTAssertEqual(try! SemVer(string: "1.0.0").increasing(majorBy: Int.max, minorBy: Int.min, patch: 9999),
                       try! SemVer(string: "9223372036854775808.0.9999"))
    }
}
