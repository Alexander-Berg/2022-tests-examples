//  Rewrited by Denis Malykh on 20.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

public final class SktClosureCallChecker<T> {

    public typealias ClosureType = (T) -> Void
    public typealias ClosureWithOptionalArgumentType = (T?) -> Void

    private let expectedValues: [T?]
    private var actualValues: [T?] = []
    private let equalityTest: (T, T) -> Bool

    public convenience init(
        expectedValues: [T],
        equalityTest: @escaping (T, T) -> Bool
    ) {
        self.init(
            expectedOptionalValues: expectedValues.map { $0 as T? },
            equalityTest: equalityTest
        )
    }

    public init(
        expectedOptionalValues: [T?],
        equalityTest: @escaping (T, T) -> Bool
    ) {
        expectedValues = expectedOptionalValues
        self.equalityTest = equalityTest
        actualValues.reserveCapacity(expectedValues.count)
    }

    public func getClosure() -> ClosureType {
        { [weak self] argument in
            guard let self = self else {
                XCTFail("Called when owner was released")
                return
            }

            self.actualValues.append(argument)
        }
    }

    public func getClosureWithOptionalArgument() -> ClosureWithOptionalArgumentType {
        { [weak self] argument in
            guard let self = self else {
                XCTFail("Called when owner was released")
                return
            }

            self.actualValues.append(argument)
        }
    }

    public func expect() {
        XCTAssertEqual(expectedValues.count, actualValues.count)

        zip(expectedValues, actualValues).forEach { pair in
            if let first = pair.0, let second = pair.1 {
                XCTAssert(equalityTest(first, second))
            } else {
                XCTAssertNil(pair.0)
                XCTAssertNil(pair.1)
            }
        }
    }

    public func assertClosureNotCalled() {
        guard !expectedValues.isEmpty else {
            XCTFail("Should not be called without expected parameters")
            return
        }
        XCTAssertTrue(actualValues.isEmpty)
    }
}

extension SktClosureCallChecker where T: Equatable {
    public convenience init(expectedValues: [T]) {
        self.init(expectedOptionalValues: expectedValues.map { $0 as T? })
    }

    public convenience init(expectedOptionalValues: [T?]) {
        self.init(
            expectedOptionalValues: expectedOptionalValues,
            equalityTest: ==
        )
    }
}

public final class VoidClosureCallChecker {
    private let expectedCalls: Int
    public private(set) var receivedCalls = 0

    public init(numberOfCalls: Int) {
        expectedCalls = numberOfCalls
    }

    public func getClosure() -> () -> Void {
        { [weak self] in
            guard let self = self else {
                XCTFail("Called when owner was released")
                return
            }

            self.receivedCalls += 1
        }
    }

    public func expect() {
        XCTAssertEqual(receivedCalls, expectedCalls)
    }

    public func expect(atLeast times: Int) {
        XCTAssertGreaterThanOrEqual(receivedCalls, times)
    }

    public func assertClosureNotCalled() {
        guard expectedCalls > 0 else {
            XCTFail("Should not be called without expected parameters")
            return
        }
        XCTAssertTrue(receivedCalls == 0)
    }
}
