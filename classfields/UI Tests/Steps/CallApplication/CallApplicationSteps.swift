//
//  CallApplicationSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 17.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.CallApplicationAccessibilityIdentifiers
import enum YREAccessibilityIdentifiers.ApplicationSentViewAccessibilityIdentifiers

final class CallApplicationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана формы заявки на звонок") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем закрытие экрана формы заявки на звонок") { _ -> Void in
            self.viewController.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    func typePhone(_ number: String = "1234567890") -> Self {
        XCTContext.runActivity(named: "Вводим номер телефона \(number) в форму") { _ -> Void in
            self.phoneTextField
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(number)
        }
        return self
    }

    func tapSubmit() -> Self {
        XCTContext.runActivity(named: "Нажимаем на отправку формы") { _ -> Void in
            self.submitButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isSuccessAlertPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отображение алерта успешной отправки") { _ -> Void in
            self.bottomSheetView
                .yreEnsureExistsWithTimeout()
            let successText = ElementsProvider.obtainElement(identifier: "Заявка отправлена!", in: self.bottomSheetView)
            successText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isErrorAlertPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отображение алерт НЕуспешной отправки") { _ -> Void in
            self.bottomSheetView
                .yreEnsureExistsWithTimeout()
            let errorText = ElementsProvider.obtainElement(identifier: "Не удалось отправить заявку", in: self.bottomSheetView)
            errorText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapCloseAlert() -> Self {
        XCTContext.runActivity(named: "Закрываем алерт") { _ -> Void in
            self.bottomSheetAction
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = CallApplicationAccessibilityIdentifiers
    private typealias BottomSheetIdentifiers = ApplicationSentViewAccessibilityIdentifiers

    private lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var phoneTextField = ElementsProvider.obtainElement(identifier: Identifiers.phone, in: self.viewController)
    private lazy var submitButton = ElementsProvider.obtainElement(identifier: Identifiers.submit, in: self.viewController)
    private lazy var bottomSheetView = ElementsProvider.obtainElement(identifier: BottomSheetIdentifiers.view)
    private lazy var bottomSheetAction = ElementsProvider.obtainElement(identifier: BottomSheetIdentifiers.actionButton,
                                                                        in: self.bottomSheetView)
}
