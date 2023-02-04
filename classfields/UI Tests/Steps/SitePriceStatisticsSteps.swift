//
//  SitePriceStatisticsSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 01.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class SitePriceStatisticsSteps {
    enum Room: CaseIterable {
        case studio
        case one
        case two
        case three
        case plus4

        var title: String {
            switch self {
                case .studio:
                    return "Студия"
                case .one:
                    return "1"
                case .two:
                    return "2"
                case .three:
                    return "3"
                case .plus4:
                    return "4+"
            }
        }
    }

    enum Period: CaseIterable {
        case halfYear
        case year
        case all

        var title: String {
            switch self {
                case .halfYear:
                    return "Полгода"
                case .year:
                    return "Год"
                case .all:
                    return "Всё время"
            }
        }
    }

    @discardableResult
    func ensureViewAppeared() -> Self {
        _ = XCTContext.runActivity(named: "Проверяем, что контент экрана появился") { _ in
            self.contentView.yreWaitForExistence()
        }
        return self
    }

    @discardableResult
    func chooseRoom(_ room: Room) -> Self {
        XCTContext.runActivity(named: "Выбираем комнатность \(room.title)") { _ in
            let roomButton = self.roomButton(with: "YRESegmentedControl-\(room.title)")
            self.view.scrollToElement(element: roomButton, direction: .up)
            roomButton
                .yreTap()
        }
        return self
    }

    @discardableResult
    func choosePeriod(_ period: Period) -> Self {
        XCTContext.runActivity(named: "Выбираем период \(period.title)") { _ in
            let periodButton = self.periodButton(with: period.title)
            self.view.scrollToElement(element: periodButton, direction: .up)
            periodButton
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = SitePriceStatisticsAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)

    private lazy var roomsPicker = ElementsProvider.obtainElement(
        identifier: Identifiers.roomsPicker,
        in: self.view
    )

    private lazy var periodPicker = ElementsProvider.obtainElement(
        identifier: Identifiers.periodPicker,
        in: self.view
    )

    private func roomButton(with title: String) -> XCUIElement {
        ElementsProvider.obtainElement(identifier: title, in: self.view)
    }

    private func periodButton(with title: String) -> XCUIElement {
        ElementsProvider.obtainElement(identifier: title, in: self.periodPicker)
    }
}
