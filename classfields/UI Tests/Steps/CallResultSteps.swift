//
//  CallResultSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 25.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.AbuseAccessibilityIdentifiers

final class CallResultSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Оцените звонок'") { _ -> Void in
            self.callResultView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана 'Оцените звонок'") { _ -> Void in
            self.callResultView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Закрыть\"") { _ -> Void in
            self.offerAbuseCloseButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = AbuseAccessibilityIdentifiers

    private lazy var callResultView = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Call.view,
        type: .other
    )

    private lazy var offerAbuseCloseButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Call.closeButton,
        type: .button,
        in: self.callResultView
    )
}
