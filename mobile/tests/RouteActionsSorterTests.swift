//
//  RouteActionsSorterTests.swift
//  YandexMapsSwiftUI
//
//  Created by Alexander Ermichev on 1/11/21.
//

import XCTest
import YandexMapsCommonTypes
@testable import YandexMapsSwiftUI

class RouteActionsSorterTests: XCTestCase {

    // MARK: Sorting tests

    func testSortingZeroActionsReturnsNil() {
        let input: [RouteAction] = []
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition)

        XCTAssertNil(output)
    }

    func testSortingOneActionReturnsIt() {
        let input: [RouteAction] = [
            action(kind: .home, position: farPosition)
        ]
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition)

        XCTAssertEqual(input.count, output?.sorted.count)
        XCTAssertEqual(input[0].kind, output?.preferred.kind)
        XCTAssertEqual(input[0].kind, output?.sorted[0].kind)
    }

    func testSortingTwoActionsReturnsPreferredFirst() {
        let input: [RouteAction] = [
            action(kind: .work, position: farPosition),
            action(kind: .home, position: farPosition)
        ]
        let fixedDate = Date()
        let preferredKind = RouteActionsSorter.preferredActionKind(for: fixedDate)
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition, date: fixedDate)

        XCTAssertEqual(input.count, output?.sorted.count)
        XCTAssertEqual(output?.sorted[0].kind, output?.preferred.kind)
        XCTAssertNotEqual(output?.sorted[1].kind, output?.preferred.kind)
        XCTAssertEqual(preferredKind, output?.preferred.kind)
    }

    func testSortingVeryCloseActionsReturnsNil() {
        let input: [RouteAction] = [
            action(kind: .home, position: veryClosePosition),
            action(kind: .work, position: veryClosePosition)
        ]
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition)

        XCTAssertNil(output)
    }

    func testSortingVeryCloseAndFarActionsReturnsOnlyFar() {
        let input: [RouteAction] = [
            action(kind: .home, position: veryClosePosition),
            action(kind: .work, position: farPosition)
        ]
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition)

        XCTAssertEqual(1, output?.sorted.count)
        XCTAssertEqual(output?.preferred.kind, .work)
        XCTAssertEqual(output?.sorted[0].kind, output?.preferred.kind)
    }

    func testSortingCloseAndFarActionsReturnsFarFirst() {
        let input: [RouteAction] = [
            action(kind: .work, position: closePosition),
            action(kind: .home, position: farPosition)
        ]
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition, date: weekdayDate)

        XCTAssertEqual(input.count, output?.sorted.count)
        XCTAssertEqual(output?.preferred.kind, .home)
        XCTAssertEqual(output?.sorted[0].kind, output?.preferred.kind)
    }

    func testSortingTwoCloseActionsReturnsPreferredFirst() {
        let input: [RouteAction] = [
            action(kind: .work, position: closePosition),
            action(kind: .home, position: closePosition)
        ]
        let output = RouteActionsSorter.sortedActions(from: input, userPosition: userPosition, date: weekdayDate)
        let preferredKind = RouteActionsSorter.preferredActionKind(for: weekdayDate)

        XCTAssertEqual(input.count, output?.sorted.count)
        XCTAssertEqual(preferredKind, .work)
        XCTAssertEqual(output?.preferred.kind, preferredKind)
        XCTAssertEqual(output?.sorted[0].kind, output?.preferred.kind)
    }

    // MARK: Preferred action tests

    func testPreferredActionForWeekdayIsWork() {
        let kind = RouteActionsSorter.preferredActionKind(for: weekdayDate)
        XCTAssertEqual(kind, .work)
    }

    func testPreferredActionForWeekendIsHome() {
        let kind = RouteActionsSorter.preferredActionKind(for: weekendDate)
        XCTAssertEqual(kind, .home)
    }


}

fileprivate extension RouteActionsSorterTests {

    var calendar: Calendar { Locale(identifier: "ru_RU").calendar }

    var weekdayDate: Date { calendar.date(from: DateComponents(year: 2021, month: 1, day: 11, hour: 11))! }
    var weekendDate: Date { calendar.date(from: DateComponents(year: 2021, month: 1, day: 10, hour: 11))! }

    var userPosition: Coordinate { CoordinatePoint(lat: 0.0, lon: 0.0) }
    var veryClosePosition: Coordinate { CoordinatePoint(lat: 0.0, lon: 0.0001) }
    var closePosition: Coordinate { CoordinatePoint(lat: 0.0, lon: 0.005) }
    var farPosition: Coordinate { CoordinatePoint(lat: 0.0, lon: 0.1) }

    func action(kind: ImportantPlaceKind, position: Coordinate) -> RouteAction {
        return RouteAction(kind: kind, position: position, time: "", linkUrl: validUrl)
    }

    var validUrl: URL {
        return URL(string: "yandexmaps://widget")!
    }

}
