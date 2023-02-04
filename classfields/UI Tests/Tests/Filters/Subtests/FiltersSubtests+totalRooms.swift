//
//  FiltersSubtests+totalRooms.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension FiltersSubtests {
    enum RoomsTotal: CaseIterable {
        case studio
        case rooms1
        case rooms2
        case rooms3
        case rooms4Plus
    }

    func totalRooms() {
        let runKey = #function
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - пропускаем 'Количество комнат'"
            XCTContext.runActivity(named: activityName) { _ -> Void in }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true
        
        for (index, item) in RoomsTotal.allCases.enumerated() {
            let activityTitle = "Добавляем опцию '\(item.readableName)'"

            XCTContext.runActivity(named: activityTitle) { _ in
                let expectation = XCTestExpectation(description: activityTitle)

                self.checkRoomsButton(index: index, rooms: item, select: true, completion: {
                    expectation.fulfill()
                })

                let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
                XCTAssert(result)
            }
        }

        for (index, item) in RoomsTotal.allCases.enumerated().reversed() {
            let activityTitle = "Убираем опцию '\(item.readableName)'"

            XCTContext.runActivity(named: activityTitle) { _ in
                let expectation = XCTestExpectation(description: activityTitle)

                self.checkRoomsButton(index: index, rooms: item, select: false, completion: {
                    expectation.fulfill()
                })

                let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
                XCTAssert(result)
            }
        }
    }

    private func checkRoomsButton(index: Int, rooms: RoomsTotal, select: Bool, completion: @escaping () -> Void) {
        let roomsQueryKey = "roomsTotal"

        let selectedRooms = RoomsTotal.allCases.prefix(through: index + (select ? 0 : -1))
        let deselectedRooms = RoomsTotal.allCases.suffix(from: index + 1)

        let requiredQueryItems = selectedRooms.map { URLQueryItem(name: roomsQueryKey, value: $0.queryItemValue) }
        let prohibitedQueryItemsKeys = selectedRooms.isEmpty ? [roomsQueryKey] : []
        let deselectedRoomsQueryItems = deselectedRooms.map { URLQueryItem(name: roomsQueryKey, value: $0.queryItemValue) }
        let prohibitedQueryItems = selectedRooms.isEmpty ? [] : deselectedRoomsQueryItems

        self.api.setupSearchCounter(
            predicate: .queryItems(
                contain: Set(requiredQueryItems),
                notContainKeys: Set(prohibitedQueryItemsKeys),
                notContain: Set(prohibitedQueryItems)
            ),
            handler: completion
        )

        self.filtersSteps.tapOnRoomsTotalButton(rooms)
    }
}

extension FiltersSubtests.RoomsTotal {
    var queryItemValue: String {
        switch self {
            case .studio: return "STUDIO"
            case .rooms1: return "1"
            case .rooms2: return "2"
            case .rooms3: return "3"
            case .rooms4Plus: return "PLUS_4"
        }
    }

    var readableName: String {
        switch self {
            case .studio: return "Студия"
            case .rooms1: return "1 комната"
            case .rooms2: return "2 комнаты"
            case .rooms3: return "3 комнаты"
            case .rooms4Plus: return "4+ комнат"
        }
    }

    var buttonTitle: String {
        switch self {
            case .studio: return "Студия"
            case .rooms1: return "1"
            case .rooms2: return "2"
            case .rooms3: return "3"
            case .rooms4Plus: return "4+"
        }
    }
}
