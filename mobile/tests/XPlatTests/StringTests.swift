//
//  StringTests.swift
//  XMailTests
//
//  Created by Aleksandr A. Dvornikov on 256//19.
//

import XCTest
import XPlat

class StringTests: XCTestCase {
  func testShouldCorrectlySliceStrings() {
    let str = "The quick brown fox jumps over the lazy dog."

    XCTAssertEqual(str.slice(31), "the lazy dog.")
    XCTAssertEqual(str.slice(4, 19), "quick brown fox")
    XCTAssertEqual(str.slice(-4), "dog.")
    XCTAssertEqual(str.slice(-9, -5), "lazy")
    XCTAssertEqual(str.slice(31, -5), "the lazy")
    XCTAssertEqual(str.slice(300), "")
    XCTAssertEqual(str.slice(31, 26), "")
    XCTAssertEqual(str.slice(), "The quick brown fox jumps over the lazy dog.")
  }

  func testShouldCorrectlySubstringStrings() {
    let str = "Mozilla"

    XCTAssertEqual(str.substring(0, 1), "M")
    XCTAssertEqual(str.substring(1, 0), "M")
    XCTAssertEqual(str.substring(0, 6), "Mozill")
    XCTAssertEqual(str.substring(-1, 6), "Mozill")
    XCTAssertEqual(str.substring(4), "lla")
    XCTAssertEqual(str.substring(4, 7), "lla")
    XCTAssertEqual(str.substring(7, 4), "lla")
    XCTAssertEqual(str.substring(-5, 2), "Mo")
    XCTAssertEqual(str.substring(-5, -2), "")
    XCTAssertEqual(str.substring(10), "")
    XCTAssertEqual(str.substring(10, 12), "")
    XCTAssertEqual(str.substring(10, -12), "Mozilla")
  }

  func testShouldCorrectlySubstrStrings() {
    let str = "Mozilla"

    XCTAssertEqual(str.substr(0, 1), "M")
    XCTAssertEqual(str.substr(1, 0), "")
    XCTAssertEqual(str.substr(-1, 1), "a")
    XCTAssertEqual(str.substr(1, -1), "")
    XCTAssertEqual(str.substr(-3), "lla")
    XCTAssertEqual(str.substr(1), "ozilla")
    XCTAssertEqual(str.substr(-20, 2), "Mo")
    XCTAssertEqual(str.substr(20, 2), "")
  }

  func testShouldFindSubstringByRegex() {
    let str = "Quick brown fox jumps over the lazy dog"
    let regex = "^([^@]+)@(ya(?:ndex-team|money)\\.(?:ru|com(\\.(tr|ua))?))$"

    XCTAssertEqual(str.search("o"), 8)
    XCTAssertEqual(str.search("abc"), -1)
    XCTAssertNotEqual("comfly@yandex-team.ru".search(regex), -1)
  }

  func testShouldFindIndexOfSubstring() {
    let str = "Quick brown fox jumps over the lazy dog"
    XCTAssertEqual(str.indexOf("brown"), 6)
    XCTAssertEqual(str.indexOf("brown", 6), 6)
    XCTAssertEqual(str.indexOf("brown", 7), -1)

    XCTAssertEqual(str.indexOf("abc"), -1)
    XCTAssertEqual(str.indexOf("abc", str.length - 2), -1)
    XCTAssertEqual(str.indexOf("abc", str.length + 2), -1)

    XCTAssertEqual(str.indexOf(""), 0)
    XCTAssertEqual(str.indexOf("", 2), 2)
    XCTAssertEqual(str.indexOf("", str.length + 1), str.length)

    XCTAssertEqual("".indexOf(""), 0)
    XCTAssertEqual("".indexOf("", 2), 0)
    XCTAssertEqual("".indexOf("a"), -1)
    XCTAssertEqual("".indexOf("a", 8), -1)
  }
  
  func testShouldRepeatStrings() {
    let str = "Happy"
    XCTAssertEqual(str.repeat(0), "")
    XCTAssertEqual(str.repeat(1), "Happy")
    XCTAssertEqual(str.repeat(2), "HappyHappy")
  }
  
  func testShouldPadStartStrings() {
    XCTAssertEqual("abc".padStart(10), "       abc")
    XCTAssertEqual("abc".padStart(10, "foo"), "foofoofabc")
    XCTAssertEqual("abc".padStart(6,"123465"), "123abc")
    XCTAssertEqual("abc".padStart(8, "0"), "00000abc")
    XCTAssertEqual("abc".padStart(1), "abc")
  }

  func testRegex() {
    XCTAssertEqual("foo".match("foo")!.items, ["foo"])
    XCTAssertNil("foo".match("bar"))
    XCTAssertEqual("/payments/123/markup".match("/payments/([0-9]+)/markup")!.items, ["/payments/123/markup", "123"])
    XCTAssertEqual("1my 2own 3string".match("(([0-9]+)[a-z]+) ")!.items, ["1my ", "1my", "1"])
  }
}
