//
//  ArrayDiffTests.swift
//  YandexMaps
//
//  Created by Viacheslav Gilevich on 18.05.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapsUtils

class ArrayDiffTests: XCTestCase {
    
    func testEmpty() {
        let diff = ArrayDiff<Int>(lhs: [], rhs: [])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 0)
    }
    
    func testNotChanged() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [1, 2, 3])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 0)
    }
    
    func testZeroToOne() {
        let diff = ArrayDiff(lhs: [], rhs: [1])
        XCTAssert(diff.added.count == 1)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 0)
        XCTAssert(diff.added[0] == 0)
    }
    
    func testOneToZero() {
        let diff = ArrayDiff(lhs: [1], rhs: [])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 1)
        XCTAssert(diff.moved.count == 0)
        XCTAssert(diff.removed[0] == 0)
    }
    
    func testReversed() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [3, 2, 1])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 2)
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 0, to: 2) }))
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 2, to: 0) }))
    }
    
    func testAddOne() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [0, 1, 2, 3])
        XCTAssert(diff.added.count == 1)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 3)
        XCTAssert(diff.added[0] == 0)
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 0, to: 1) }))
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 1, to: 2) }))
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 2, to: 3) }))
    }

    func testInconsistentMove() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [0, 2, 3, 1])
        XCTAssert(diff.added.count == 1)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 1)
        XCTAssert(diff.added[0] == 0)
    }

    func testMissedMove() {
        let origin = [1, 2, 3, 4, 5]
        let destination = [5, 4, 3]
        let diff = ArrayDiff(lhs: origin, rhs: destination)
        let newElements = diff.added.map { destination[$0] }
        guard let applicable = ApplicableArrayDiff(diff: diff, newElements: newElements) else {
            XCTAssert(false)
            return
        }
        let tmp = origin.apply(diff: applicable)
        XCTAssert(tmp == destination)
    }
    
    func testRemoveOne() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [2, 3])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 1)
        XCTAssert(diff.moved.count == 0)
        XCTAssert(diff.removed[0] == 0)
    }
    
    func testMoveOne() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [2, 3, 1])
        XCTAssert(diff.added.count == 0)
        XCTAssert(diff.removed.count == 0)
        XCTAssert(diff.moved.count == 3)
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 0, to: 2) }))
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 1, to: 0) }))
        XCTAssert(diff.moved.contains(where: { $0 == ArrayDiff<Int>.MovedIndices(from: 2, to: 1) }))
    }
    
    func testChangeOne() {
        let diff = ArrayDiff(lhs: [1, 2, 3], rhs: [1, 4, 3])
        XCTAssert(diff.added.count == 1)
        XCTAssert(diff.removed.count == 1)
        XCTAssert(diff.moved.count == 0)
        XCTAssert(diff.added[0] == 1)
        XCTAssert(diff.removed[0] == 1)
    }
    
    func testApplicableDiffRepeatingInsertion() {
        let origin = [1, 2, 3, 4, 5, 6, 7, 8, 9]
        let destination = [7, 7, 7, 6, 7, 2, 3, 7, 4, 5, 7, 7, 8, 1, 9]
        let diff = ArrayDiff(lhs: origin, rhs: destination)
        let applicable = ApplicableArrayDiff(diff: diff, repeating: 7)
        XCTAssert(origin.apply(diff: applicable) == destination)
    }
    
    func testRandomArrayDifference() {
        for _ in 0..<10 {
            let origin = (0..<arc4random_uniform(20)).map { _ in arc4random_uniform(10) }
            let destination = (0..<arc4random_uniform(20)).map { _ in arc4random_uniform(10) }
            let diff = ArrayDiff(lhs: origin, rhs: destination)
            let newElements = diff.added.map { destination[$0] }
            guard let applicable = ApplicableArrayDiff(diff: diff, newElements: newElements) else {
                XCTAssert(false)
                return
            }
            XCTAssert(origin.apply(diff: applicable) == destination)
        }
    }
    
}
