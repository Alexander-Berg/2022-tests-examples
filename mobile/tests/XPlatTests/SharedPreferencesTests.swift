//
//  SharedPreferencesTests.swift
//  XMailTests
//
//  Created by Dmitry Zakharov on 30/05/2019.
//

import XCTest
@testable import XPlat

private final class TestUserDefaults: UserDefaultsProvider {
  public var dictionary: [String: Any]
  init(dictionary: [String: Any]) {
    self.dictionary = dictionary
  }

  func dictionary(forKey key: String) -> [String: Any]? {
    return dictionary[key] as? [String: Any]
  }

  func set(_ value: Any?, forKey key: String) {
    dictionary[key] = value
  }
}

class SharedPreferencesTests: XCTestCase {
  func testSharedPreferencesCanGetValues() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: [
      "NAME": [
        "Int32": Int64(10),
        "Int64": Int64(20),
        "Double": 10.5,
        "Bool": true,
        "String": "String",
        "Set": ["1", "2"],
      ],
    ]))
    let prefs = provider.sharedPreferencesWithName("NAME")
    XCTAssertTrue(prefs.contains("Int32"))
    XCTAssertEqual(prefs.getInt32("Int32", 1), 10)
    XCTAssertEqual(prefs.getInt64("Int64", 1), 20 as Int64)
    XCTAssertEqual(prefs.getDouble("Double", 1.0), 10.5)
    XCTAssertEqual(prefs.getString("String", "D"), "String")
    XCTAssertEqual(prefs.getBoolean("Bool", false), true)
    XCTAssertEqual(prefs.getStringSet("Set", YSSet(["A", "B"])).items, YSSet(["1", "2"]).items)
  }

  func testSharedPreferencesCanGetDefaultValuesIfAbsent() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: ["NAME": [:]]))
    let prefs = provider.sharedPreferencesWithName("NAME")
    XCTAssertFalse(prefs.contains("Int32"))
    XCTAssertEqual(prefs.getInt32("Int32", 1), 1)
    XCTAssertEqual(prefs.getInt64("Int64", 2), 2 as Int64)
    XCTAssertEqual(prefs.getDouble("Double", 1.5), 1.5)
    XCTAssertEqual(prefs.getString("String", "Def"), "Def")
    XCTAssertEqual(prefs.getBoolean("Bool", true), true)
    XCTAssertEqual(prefs.getStringSet("Set", YSSet(["A", "B"])).items, YSSet(["A", "B"]).items)
  }

  func testSharedPreferencesShouldCreateNewIfUnknownName() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: [
      "NAME1": [
        "Int32": Int64(10),
      ],
    ]))
    let prefs = provider.sharedPreferencesWithName("NAME2")
    XCTAssertFalse(prefs.contains("Int32"))
  }

  func testSharedPreferencesShouldOverwriteValues() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: [
      "NAME": [
        "Int32": Int64(10),
        "Int64": Int64(20),
        "Double": 10.5,
        "Bool": true,
        "String": "String",
        "Set": ["1", "2"],
        "Extra": "Extra field",
      ],
    ]))
    let prefs = provider.sharedPreferencesWithName("NAME")
    let editor = prefs.edit()
      .putInt32("Int32", 20)
      .putInt64("Int64", 40)
      .putDouble("Double", 20.5)
      .putBoolean("Bool", false)
      .putString("String", "Other")
      .putStringSet("Set", YSSet(["A", "B"]))
      .remove("Extra")

    // Not changed before commit()
    XCTAssertEqual(prefs.getInt32("Int32", 1), 10)
    XCTAssertEqual(prefs.getInt64("Int64", 2), 20 as Int64)
    XCTAssertEqual(prefs.getDouble("Double", 1.5), 10.5)
    XCTAssertEqual(prefs.getString("String", "Def"), "String")
    XCTAssertEqual(prefs.getBoolean("Bool", false), true)
    XCTAssertEqual(prefs.getStringSet("Set", YSSet(["A", "B"])).items, YSSet(["1", "2"]).items)

    // Changed after commit
    editor.commit()
    XCTAssertEqual(prefs.getInt32("Int32", 1), 20)
    XCTAssertEqual(prefs.getInt64("Int64", 2), 40 as Int64)
    XCTAssertEqual(prefs.getDouble("Double", 1.5), 20.5)
    XCTAssertEqual(prefs.getString("String", "Def"), "Other")
    XCTAssertEqual(prefs.getBoolean("Bool", true), false)
    XCTAssertEqual(prefs.getStringSet("Set", YSSet(["1", "2"])).items, YSSet(["A", "B"]).items)
    XCTAssertFalse(prefs.contains("Extra"))
  }

  func testSharedPreferencesShouldPutNewValues() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: ["NAME1": []]))
    let prefs = provider.sharedPreferencesWithName("NAME2")
    prefs.edit()
      .putInt32("Int32", 20)
      .putInt64("Int64", 40)
      .putDouble("Double", 20.5)
      .putBoolean("Bool", true)
      .putString("String", "String")
      .putStringSet("Set", YSSet(["A", "B"]))
      .commit()

    let check = provider.sharedPreferencesWithName("NAME2")
    // Changed after commit
    XCTAssertEqual(check.getInt32("Int32", 1), 20)
    XCTAssertEqual(check.getInt64("Int64", 2), 40 as Int64)
    XCTAssertEqual(check.getDouble("Double", 1.5), 20.5)
    XCTAssertEqual(check.getString("String", "Def"), "String")
    XCTAssertEqual(check.getBoolean("Bool", false), true)
    XCTAssertEqual(check.getStringSet("Set", YSSet(["1", "2"])).items, YSSet(["A", "B"]).items)
  }

  func testSharedPreferencesShouldApplyValues() {
    let provider = DefaultSharedPreferencesProvider(userDefaultsProvider: TestUserDefaults(dictionary: ["NAME1": []]))
    let prefs = provider.sharedPreferencesWithName("NAME2")
    prefs.edit()
      .putInt32("Int32", 20)
      .putInt64("Int64", 40)
      .putDouble("Double", 20.5)
      .putBoolean("Bool", true)
      .putString("String", "String")
      .putStringSet("Set", YSSet(["A", "B"]))
      .commit()

    let check = provider.sharedPreferencesWithName("NAME2")
    // Changed after commit
    XCTAssertEqual(check.getInt32("Int32", 1), 20)
    XCTAssertEqual(check.getInt64("Int64", 2), 40 as Int64)
    XCTAssertEqual(check.getDouble("Double", 1.5), 20.5)
    XCTAssertEqual(check.getString("String", "Def"), "String")
    XCTAssertEqual(check.getBoolean("Bool", false), true)
    XCTAssertEqual(check.getStringSet("Set", YSSet(["1", "2"])).items, YSSet(["A", "B"]).items)
  }
}
