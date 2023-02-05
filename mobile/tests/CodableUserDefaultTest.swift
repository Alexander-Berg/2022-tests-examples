//
//  CodableUserDefaultTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 17.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import TestUtils
@testable import Utils

public final class CodableUserDefaultTest: XCTestCase {
    private struct TestCodableModel: Codable, Equatable, Hashable {
        let text: String
    }

    func testShouldSaveAndLoad() {
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: TestCodableModel(text: "test"),
                                                newValue: TestCodableModel(text: "new")))
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: [TestCodableModel(text: "test")],
                                                newValue: [TestCodableModel(text: "new")]))
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: ["first": TestCodableModel(text: "test")],
                                                newValue: ["second": TestCodableModel(text: "new")]))
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: Set([TestCodableModel(text: "test")]),
                                                newValue: Set([TestCodableModel(text: "new")])))

        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: ["test"],
                                                newValue: ["new"]))
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: ["first": "test"],
                                                newValue: ["second": "new"]))
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: Set(["test"]),
                                                newValue: Set(["new"])))

        let optionalInitial: String? = "Old"
        let optionalNew: String? = "Old"
        XCTAssertTrue(self.checkSaveAndLoadWith(initialValue: optionalInitial, newValue: optionalNew))
    }

    func testShouldNotSaveAndLoad() {
        XCTAssertFalse(self.checkSaveAndLoadWith(initialValue: "1", newValue: "2"))
        XCTAssertFalse(self.checkSaveAndLoadWith(initialValue: 1.0, newValue: 2.0))
        XCTAssertFalse(self.checkSaveAndLoadWith(initialValue: false, newValue: true))
        XCTAssertFalse(self.checkSaveAndLoadWith(initialValue: 1, newValue: 2))
    }

    private func checkSaveAndLoadWith<T: Codable & Equatable>(initialValue: T, newValue: T) -> Bool {
        let testUserDefault = UserDefaults(suiteName: #function)!
        let key = "test_key"
        let defaultValue = initialValue
        let someSavedValue = CodableUserDefault(key: key, defaultValue: defaultValue, userDefaults: testUserDefault)

        let loadedInitialValue = CodableUserDefault(key: key, defaultValue: defaultValue, userDefaults: testUserDefault)
        XCTAssertEqual(loadedInitialValue.wrappedValue, initialValue)

        someSavedValue.wrappedValue = newValue

        let someLoadedValue = CodableUserDefault(key: key, defaultValue: defaultValue, userDefaults: testUserDefault)
        return someLoadedValue.wrappedValue == newValue
    }
}
