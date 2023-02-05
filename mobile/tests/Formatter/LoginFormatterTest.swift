//
//  LoginFormatterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 29.07.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class LoginFormatterTest: XCTestCase {
    private var loginFormatter: LoginFormatter! = LoginFormatter()

    func testInvalidChars() {
        ["кириллица", "кириллицаlatin"].forEach { login in
            self.check(login: login, error: [.invalidChars])
        }
        Array("+=_)(*,^&%№!кириллица").forEach { symbol in
            self.check(login: "fail\(String(symbol))login", error: [.invalidChars])
        }

        Array(".-0123456789").forEach { symbol in
            self.check(login: "normal\(String(symbol))login", error: [])
        }
    }

    func testInvalidPrefix() {
        Array(".-0123456789").forEach { symbol in
            self.check(login: "\(String(symbol))notCorrectPrefix", error: [.invalidLoginPrefix])
            self.check(login: String(symbol), error: [.invalidLoginPrefix])
        }

        Array("CorrectPrefix").forEach { symbol in
            self.check(login: "\(String(symbol))notCorrectPrefix", error: [])
        }
    }

    func testInvalidSuffix() {
        Array(".-").forEach { symbol in
            self.check(login: "notCorrectSuffix\(String(symbol))", error: [.invalidLoginSuffix])
        }

        Array("CorrectPrefix").forEach { symbol in
            self.check(login: "notCorrectSuffix\(String(symbol))", error: [])
        }
    }

    func testDotAndDashStandInARow() {
        [".-", "-."].forEach { text in
            self.check(login: "not\(text)valid", error: [.invalidLoginDotAndDash])
        }

        ["valid.login.test", "valid-login-test", "valid.login-test", "valid-login.test"].forEach { login in
            self.check(login: login, error: [])
        }
    }

    func testDoubleDots() {
        self.check(login: "not..valid", error: [.invalidLoginDoubleDots])
        self.check(login: "not...valid", error: [.invalidLoginDoubleDots])

        ["valid.login.test", "valid-login-test", "valid.login-test", "valid-login.test"].forEach { login in
            self.check(login: login, error: [])
        }
    }

    func testDoubleDashes() {
        self.check(login: "not--valid", error: [.invalidLoginDoubleDashes])
        self.check(login: "not---valid", error: [.invalidLoginDoubleDashes])

        ["valid.login.test", "valid-login-test", "valid.login-test", "valid-login.test"].forEach { login in
            self.check(login: login, error: [])
        }
    }

    func testSomeErrorsTogether() {
        Array(".-0123456789").forEach { symbol in
            self.check(login: "\(String(symbol))кириллица", error: [.invalidChars, .invalidLoginPrefix])
        }
        Array(".-").forEach { symbol in
            self.check(login: "кириллица\(String(symbol))", error: [.invalidChars, .invalidLoginSuffix])
        }
        self.check(login: ".кириллица.", error: [.invalidChars, .invalidLoginPrefix, .invalidLoginSuffix])
        self.check(login: "..", error: [.invalidLoginPrefix, .invalidLoginSuffix, .invalidLoginDoubleDots])
        self.check(login: ".кириллица.-latin.", error: [.invalidChars, .invalidLoginPrefix, .invalidLoginSuffix, .invalidLoginDotAndDash])
        self.check(login: "--latin", error: [.invalidLoginPrefix, .invalidLoginDoubleDashes])
        self.check(login: "latin..", error: [.invalidLoginSuffix, .invalidLoginDoubleDots])
    }

    private func check(login: String, error: [FormatterError]) {
        XCTAssertEqual(self.loginFormatter.check(login), error)
    }

    override func tearDown() {
        self.loginFormatter = nil

        super.tearDown()
    }
}
