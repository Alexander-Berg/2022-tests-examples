//
//  Created by Alexey Aleshkov on 27/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils

final class BindBankCard3dsAuthenticationSteps: WebPageSteps {
    lazy var phoneNumberField = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.webPhoneNumberFieldIdentifier,
        type: .textField,
        in: self.webView
    )

    lazy var confirmButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.webConfirmButtonIdentifier,
        type: .button,
        in: self.webView
    )

    // MARK: Private

    private enum AccessibilityIdentifiers {
        static let webPhoneNumberFieldIdentifier = "Any number"
        static let webConfirmButtonIdentifier = "Confirm"
    }

    private enum Consts {
        static let webPageTimeout: TimeInterval = 10
    }
}

extension BindBankCard3dsAuthenticationSteps {
    @discardableResult
    func fillForm(phoneNumber: String) -> Self {
        self.phoneNumberField
            .yreEnsureExistsWithTimeout(timeout: Consts.webPageTimeout)
            .yreTap()
            .yreTypeText(phoneNumber)
        return self
    }

    @discardableResult
    func tapOnSubmit() -> Self {
        self.confirmButton
            .yreEnsureExistsWithTimeout(timeout: Consts.webPageTimeout)
            .yreTap()
        return self
    }
}

extension BindBankCard3dsAuthenticationSteps {
    func makeActivity() -> BindBankCard3dsAuthStepsActivity {
        return .init(self)
    }
}

final class BindBankCard3dsAuthStepsActivity {
    init(_ steps: BindBankCard3dsAuthenticationSteps) {
        self.steps = steps
    }

    @discardableResult
    func fillForm(phoneNumber: String) -> Self {
        self.phoneNumber = phoneNumber
        self.isSuccess = true

        return self
    }

    @discardableResult
    func cancel() -> Self {
        self.isSuccess = false

        return self
    }

    @discardableResult
    func run() -> Self {
        let isSuccess = YREUnwrap(self.isSuccess)

        XCTContext.runActivity(named: "3D secure authentication", block: { _ -> Void in
            if isSuccess {
                self.performSubmit()
            }
            else {
                self.performCancel()
            }
        })

        return self
    }

    private let steps: BindBankCard3dsAuthenticationSteps
    private var isSuccess: Bool?
    private var phoneNumber: String?

    private func performSubmit() {
        let phoneNumber = YREUnwrap(self.phoneNumber)

        self.steps
            .screenIsPresented()
            .fillForm(phoneNumber: phoneNumber)
            .tapOnSubmit()
            .screenIsDismissed()
    }

    private func performCancel() {
        self.steps
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
    }
}
