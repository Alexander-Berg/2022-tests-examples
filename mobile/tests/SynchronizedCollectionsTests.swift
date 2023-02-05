//
//  SynchronizedCollectionsTests.swift
//  YandexMapsTests
//
//  Created by Alexander Ermichev on 07/08/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapsUtils

class SynchronizedCollectionsTests: XCTestCase {

    // MARK: - Public methods

    override func setUp() {
        super.setUp()

        dict.removeAll(async: false)
        operationsCount = 0
    }

    func testSynchronizedDictionarySyncWrite() {
        let expectation = XCTestExpectation(description: #function)
        expectation.expectedFulfillmentCount = 2 * Static.testOperationsCount

        let operationBlock: (Int) -> Void = { index in
            self.dict[sync: "test"] = index
            DispatchQueue.main.async {
                self.operationsCount += 1
                expectation.fulfill()
            }
        }

        runConcurrentLoop(on: .main, block: operationBlock)
        runConcurrentLoop(on: .global(), block: operationBlock)

        wait(for: [expectation], timeout: 10.0)
        XCTAssertEqual(operationsCount, 2 * Static.testOperationsCount)
    }

    func testSynchronizedDictionaryAsyncWrite() {
        let expectation = XCTestExpectation(description: #function)
        expectation.expectedFulfillmentCount = 2 * Static.testOperationsCount

        let operationBlock: (Int) -> Void = { index in
            self.dict[async: "test"] = index
            DispatchQueue.main.async {
                self.operationsCount += 1
                expectation.fulfill()
            }
        }

        runConcurrentLoop(on: .main, block: operationBlock)
        runConcurrentLoop(on: .global(), block: operationBlock)

        wait(for: [expectation], timeout: 10.0)
        XCTAssertEqual(operationsCount, 2 * Static.testOperationsCount)
    }

    func testSynchronizedDictionaryRemove() {
        let expectation = XCTestExpectation(description: #function)
        expectation.expectedFulfillmentCount = 2 * Static.testOperationsCount

        let testDict: [String: Int] = ["test": 0]

        runConcurrentLoop(on: .main) { index in
            if index % 2 == 0 {
                self.dict.syncDict = testDict
            } else {
                self.dict.removeAll(async: index % 3 == 0)
            }

            DispatchQueue.main.async {
                self.operationsCount += 1
                expectation.fulfill()
            }
        }

        runConcurrentLoop(on: .global()) { index in
            if index % 2 == 1 {
                self.dict.syncDict = testDict
            } else {
                self.dict.removeAll(async: index % 3 == 0)
            }

            DispatchQueue.main.async {
                self.operationsCount += 1
                expectation.fulfill()
            }
        }

        wait(for: [expectation], timeout: 10.0)
        XCTAssertEqual(operationsCount, 2 * Static.testOperationsCount)
    }

    // MARK: - Private properties

    private let dict = SynchronizedDictionary<String, Int>()
    private var operationsCount: Int = 0
}

fileprivate extension SynchronizedCollectionsTests {

    // MARK: - Private nested types

    private enum Static {
        static let testOperationsCount = 10000
    }

    // MARK: - Private methods

    private func runConcurrentLoop(on queue: DispatchQueue, block: @escaping (Int) -> Void) {
        queue.async {
            DispatchQueue.concurrentPerform(iterations: Static.testOperationsCount, execute: block)
        }
    }

}
