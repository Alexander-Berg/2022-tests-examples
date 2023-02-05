//
//  DomainFormatterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 30.07.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class DomainFormatterTest: XCTestCase {
    private var domainFormatter: DomainFormatter! = DomainFormatter()

    func testInvalidChars() {
        ["кириллица", "кириллицаlatin"].forEach { login in
            self.check(domain: login, error: [.invalidChars])
        }
        Array("+=_)(*,^&%№!кириллица").forEach { symbol in
            self.check(domain: "fail\(String(symbol))domain", error: [.invalidChars])
        }

        Array("-0123456789").forEach { symbol in
            self.check(domain: "normal\(String(symbol))domain", error: [])
        }
    }

    func testInvalidPrefix() {
        Array("-").forEach { symbol in
            self.check(domain: "\(String(symbol))notCorrectPrefix", error: [.invalidDomainPrefix])
            self.check(domain: String(symbol), error: [.invalidDomainPrefix])
        }

        Array("CorrectPrefix1234567890").forEach { symbol in
            self.check(domain: "\(String(symbol))notCorrectPrefix", error: [])
        }
    }

    func testInvalidSuffix() {
        Array("-").forEach { symbol in
            self.check(domain: "notCorrectSuffix\(String(symbol))", error: [.invalidDomainSuffix])
        }

        Array("CorrectPrefix1234567890").forEach { symbol in
            self.check(domain: "notCorrectSuffix\(String(symbol))", error: [])
        }
    }

    func testDoubleDashesStandInARow() {
        ["--", "---", "-----"].forEach { text in
            self.check(domain: "not\(text)valid", error: [.invalidDomainDoubleDashesStandInARow])
        }

        ["valid-domain-test", "valid-domain-test-success", "valid-test"].forEach { domain in
            self.check(domain: domain, error: [])
        }
    }

    func testContainsDots() {
        ["notvalid.domain.test", "domain.", ".domain", "..domain", "d.o.m.a.i.n"].forEach { domain in
            self.check(domain: domain, error: [.invalidDomainDots])
        }
    }

    func testSomeErrorsTogether() {
        self.check(domain: "-кириллица", error: [.invalidChars, .invalidDomainPrefix])
        self.check(domain: "кириллица-", error: [.invalidChars, .invalidDomainSuffix])
        self.check(domain: "-кириллица-", error: [.invalidChars, .invalidDomainPrefix, .invalidDomainSuffix])
        self.check(domain: "-.", error: [.invalidDomainDots, .invalidDomainPrefix])
        self.check(domain: "-кириллица--latin-", error: [.invalidChars, .invalidDomainDoubleDashesStandInARow, .invalidDomainPrefix, .invalidDomainSuffix])
        self.check(domain: "--latin", error: [.invalidDomainDoubleDashesStandInARow, .invalidDomainPrefix])
        self.check(domain: "latin--", error: [.invalidDomainDoubleDashesStandInARow, .invalidDomainSuffix])
    }

    private func check(domain: String, error: [FormatterError]) {
        XCTAssertEqual(self.domainFormatter.check(domain), error)
    }

    override func tearDown() {
        self.domainFormatter = nil

        super.tearDown()
    }
}
