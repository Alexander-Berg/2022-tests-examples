//
//  CommunicationSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/26/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class CommunicationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Общение'") { _ -> Void in
            // FIXME: uncomment it
            // https://st.yandex-team.ru/VSAPPS-8757
            // self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCallHistorySegment() -> Self {
        self.tapOnSegment(title: "История звонков")
        return self
    }

    @discardableResult
    func tapOnChatsSegment() -> Self {
        self.tapOnSegment(title: "Сообщения")
        return self
    }

    // MARK: - Private
    typealias Identifiers = CommunicationAccessibilityIdentifiers
    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private func tapOnSegment(title: String) {
        XCTContext.runActivity(named: "Нажимаем на '\(title)'") { _ -> Void in

            let navbar = ElementsProvider.obtainNavigationBar()
            navbar
                .yreEnsureExistsWithTimeout()
            let segment = ElementsProvider.obtainElement(identifier: title, in: navbar)
            segment
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
    }
}
