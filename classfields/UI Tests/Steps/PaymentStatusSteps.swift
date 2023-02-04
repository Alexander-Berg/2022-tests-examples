//
//  PaymentStatusSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 13.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.PaymentStatusAccessibilityIdentifiers

final class PaymentStatusSteps {
    enum ActionKind: Equatable {
        case done
        case openExcerptList
        case changePaymentMethod
        case retry
        case openSettings
        case requestPushPermissions
        case close
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана со статусом оплаты") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func loadingIsFinished() -> Self {
        XCTContext.runActivity(named: "Ждём, когда скроется экран с загрузкой") { _ -> Void in
            ElementsProvider.obtainElement(identifier: Identifiers.loadingView)
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tryExecuteAction(_ actionKind: ActionKind) -> Self {
        XCTContext.runActivity(named: actionKind.actionDescription) { _ -> Void in
            ElementsProvider.obtainButton(identifier: actionKind.accessibilityIdentifier)
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func snapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Делаем скриншот экрана") { _ -> Void in
            let snapshot = self.viewController.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: snapshot, identifier: identifier)
        }
        return self
    }

    private typealias Identifiers = PaymentStatusAccessibilityIdentifiers

    private lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.viewController)

    private lazy var doneButton = ElementsProvider.obtainButton(identifier: Identifiers.doneButton)
    private lazy var closeButton = ElementsProvider.obtainButton(identifier: Identifiers.closeButton)
    private lazy var openSettingsButton = ElementsProvider.obtainButton(identifier: Identifiers.openSettingsButton)
    private lazy var requestPushPermissionsButton = ElementsProvider.obtainButton(
        identifier: Identifiers.requestPushPermissionsButton
    )
}

extension PaymentStatusSteps.ActionKind {
    fileprivate var actionDescription: String {
        switch self {
            case .done:
                return "Нажимаем на кнопку 'Понятно' и закрываем экран"
            case .openExcerptList:
                return "Нажимаем на кнопку 'Перейти в список отчётов'"
            case .changePaymentMethod:
                return "Нажимаем на кнопку 'Понятно' и открывает экран со списком методов оплаты"
            case .retry:
                return "Нажимаем на кнопку 'Повторить'"
            case .openSettings:
                return "Нажимаем на кнопку 'Да' и открываем настройки"
            case .requestPushPermissions:
                return "Нажимаем на кнопку 'Да' и запрашиваем разрешение на отправку пуш-уведомлений"
            case .close:
                return "Нажимаем на кнопку 'Нет' и закрываем экран"
        }
    }

    fileprivate var accessibilityIdentifier: String {
        switch self {
            case .done:
                return PaymentStatusAccessibilityIdentifiers.doneButton
            case .openExcerptList:
                return PaymentStatusAccessibilityIdentifiers.openExcerptListButton
            case .changePaymentMethod:
                return PaymentStatusAccessibilityIdentifiers.changePaymentMethodButton
            case .retry:
                return PaymentStatusAccessibilityIdentifiers.retryButton
            case .openSettings:
                return PaymentStatusAccessibilityIdentifiers.openSettingsButton
            case .requestPushPermissions:
                return PaymentStatusAccessibilityIdentifiers.requestPushPermissionsButton
            case .close:
                return PaymentStatusAccessibilityIdentifiers.closeButton
        }
    }
}
