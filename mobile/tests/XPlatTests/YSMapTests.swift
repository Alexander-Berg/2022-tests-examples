//
//  YSMapTests.swift
//  XMailTests
//
//  Created by Aleksandr A. Dvornikov on 37//19.
//

import XCTest
@testable import XPlat

class YSMapTests: XCTestCase {
  let dictionary = YSMap(items: [1: "1", 2: "2", 3: "3"])

  func testShouldCorrectlyIterateMap() {
    var count: Int32 = 0
    dictionary.__forEach { value, key in
      XCTAssertEqual(value, dictionary.get(key))
      count += 1
    }
    XCTAssertEqual(count, dictionary.size)
  }
}
