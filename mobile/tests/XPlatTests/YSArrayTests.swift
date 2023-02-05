//
//  YSArrayTests.swift
//  XMailTests
//
//  Created by Aleksandr A. Dvornikov on 36//19.
//

import XCTest
@testable import XPlat

class YSArrayTests: XCTestCase {
  let sampleArray = [1, 2, 3, 4]

  func testShouldCorrectlySliceArrays() {
    XCTAssertEqual(_ysArraySlice(sampleArray), [1, 2, 3, 4])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 1), [2, 3, 4])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 5), [])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 1, end: 3), [2, 3])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 3, end: 2), [])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: -2), [3, 4])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: -5), [1, 2, 3, 4])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 0, end: -2), [1, 2])
    XCTAssertEqual(_ysArraySlice(sampleArray, start: 0, end: -5), [])
  }

  func testShouldCorrectlySortArrays() {
    XCTAssertEqual(YSArray(array: [1, 2, 3, 4, 5]).sort { $0 - $1 }.items, [1, 2, 3, 4, 5])
    XCTAssertEqual(YSArray(array: [4, 2, 5, 1, 3]).sort { $0 - $1 }.items, [1, 2, 3, 4, 5])
    XCTAssertEqual(YSArray(array: [2, 2, 1, 1, 3]).sort { $0 - $1 }.items, [1, 1, 2, 2, 3])
    XCTAssertEqual(YSArray(array: []).sort { $0 - $1 }.items, [])
  }

  func testShouldCorrectlySpliceArrays() {
    var a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(0), YSArray(1, 2, 3, 4, 5))
    XCTAssertEqual(a, YSArray<Int>())

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(1), YSArray(2, 3, 4, 5))
    XCTAssertEqual(a, YSArray(1))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-2), YSArray(4, 5))
    XCTAssertEqual(a, YSArray(1, 2, 3))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-5), YSArray(1, 2, 3, 4, 5))
    XCTAssertEqual(a, YSArray<Int>())

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-6), YSArray(1, 2, 3, 4, 5))
    XCTAssertEqual(a, YSArray<Int>())

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(0, 0), YSArray<Int>())
    XCTAssertEqual(a, YSArray(1, 2, 3, 4, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(1, -1), YSArray<Int>())
    XCTAssertEqual(a, YSArray(1, 2, 3, 4, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(1, 1), YSArray(2))
    XCTAssertEqual(a, YSArray(1, 3, 4, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-2, 1), YSArray(4))
    XCTAssertEqual(a, YSArray(1, 2, 3, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-4, 0), YSArray<Int>())
    XCTAssertEqual(a, YSArray(1, 2, 3, 4, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-4, 2), YSArray(2, 3))
    XCTAssertEqual(a, YSArray(1, 4, 5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-5, 5), YSArray(1, 2, 3, 4, 5))
    XCTAssertEqual(a, YSArray<Int>())

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(-6, 4), YSArray(1, 2, 3, 4))
    XCTAssertEqual(a, YSArray(5))

    a = YSArray(1, 2, 3, 4, 5)
    XCTAssertEqual(a.splice(2, 3), YSArray(3, 4, 5))
    XCTAssertEqual(a, YSArray(1, 2))

    a = YSArray()
    XCTAssertEqual(a.splice(0), YSArray<Int>())
    XCTAssertEqual(a, YSArray<Int>())
  }
}

private func _ysArraySlice<T>(_ items: [T], start: Int32 = 0, end: Int32? = nil) -> [T] {
  return YSArray(array: items).slice(start, end)._items
}
