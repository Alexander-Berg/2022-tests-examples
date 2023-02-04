//
//  Created by Alexey Aleshkov on 27/02/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class BinaryRangeSearchAlgorithmTestCase: XCTestCase {
    private let fixture: [CGPoint] = [
        CGPoint(x: 160.0, y: 128.0),
        CGPoint(x: 480.0, y: 128.0),
        CGPoint(x: 800.0, y: 128.0),
        CGPoint(x: 1120.0, y: 128.0),
        CGPoint(x: 1440.0, y: 128.0)
    ]

    private func makePointComparator(_ origin: CGPoint) -> ((CGPoint) -> ComparisonResult) {
        return { point -> ComparisonResult in
            let diff = point.x - origin.x
            switch diff {
                case let x where x < 0:
                    return .orderedDescending
                case let x where x > 0:
                    return .orderedAscending
                default:
                    return .orderedSame
            }
        }
    }

    // MARK: - SearchIn

    func testBelowLowerBound() {
        let offset = CGPoint(x: 150, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == -1 && indicies.upperBound == 0)
    }

    func testExactLowerBound() {
        let offset = CGPoint(x: 160, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 0 && indicies.upperBound == 0)
    }

    func testOverLowerBound() {
        let offset = CGPoint(x: 460, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 0 && indicies.upperBound == 1)
    }

    func testAlmostInTheMiddleAbove() {
        let offset = CGPoint(x: 481, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 1 && indicies.upperBound == 2)
    }

    func testAlmostInTheMiddleBelow() {
        let offset = CGPoint(x: 799, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 1 && indicies.upperBound == 2)
    }

    func testExactInTheMiddle() {
        let offset = CGPoint(x: 800, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 2 && indicies.upperBound == 2)
    }

    func testOverUpperBound() {
        let offset = CGPoint(x: 1460.0, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        XCTAssert(indicies.lowerBound == 4 && indicies.upperBound == 5)
    }

    // MARK: - OptimizedSearchIn

    func testInTheMiddleInvalidPrevious() {
        let offset = CGPoint(x: 799, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: 3, upper: 6))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound != recalcIndicies.lowerBound || fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testInTheMiddleExactTheSamePrevious() {
        let offset = CGPoint(x: 800, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: 2, upper: 2))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testInTheMiddleSamePrevious() {
        let offset = CGPoint(x: 460, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: 0, upper: 1))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testZeroLength() {
        let emptyFixture = [] as [CGPoint]
        let offset = CGPoint(x: 150, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: emptyFixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: -1, upper: 0))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: emptyFixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testBelowLowerBoundSamePrevious() {
        let offset = CGPoint(x: 150, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: -1, upper: 0))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testBelowLowerBoundInvalidPrevious() {
        let offset = CGPoint(x: 150, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: -1, upper: 5))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound != recalcIndicies.upperBound)
    }

    func testOverUpperBoundSamePrevious() {
        let offset = CGPoint(x: 1460.0, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: 4, upper: 5))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound == recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }

    func testOverUpperBoundInvalidPrevious() {
        let offset = CGPoint(x: 1460.0, y: 128.0)
        let indicies = BinaryRangeSearchAlgorithm.searchIn(collection: self.fixture, predicate: self.makePointComparator(offset))

        let fixtureIndicies = ClosedRange(uncheckedBounds: (lower: -1, upper: 5))
        let recalcIndicies = BinaryRangeSearchAlgorithm.searchIn(
            collection: self.fixture,
            previous: fixtureIndicies,
            predicate: self.makePointComparator(offset)
        )

        XCTAssert(indicies.lowerBound == recalcIndicies.lowerBound && indicies.upperBound == recalcIndicies.upperBound)
        XCTAssert(fixtureIndicies.lowerBound != recalcIndicies.lowerBound && fixtureIndicies.upperBound == recalcIndicies.upperBound)
    }
}
