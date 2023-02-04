//
//  Created by Alexey Aleshkov on 30/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class UserEmailConfirmationSteps {
    lazy var emailView = ElementsProvider.obtainElement(
        identifier: UserEmailInputIdentifiers.viewIdentifier,
        type: .other
    )

    lazy var emailField = ElementsProvider.obtainElement(
        identifier: UserEmailInputIdentifiers.emailTextFieldIdentifier,
        type: .textField,
        in: self.emailView
    )

    lazy var emailSubmitButton = ElementsProvider.obtainElement(
        identifier: UserEmailInputIdentifiers.submitButtonIdentifier,
        type: .button,
        in: self.emailView
    )

    lazy var closeButton = ElementsProvider.obtainElement(
        identifier: UserEmailInputIdentifiers.closeButtonIdentifier,
        type: .button,
        in: self.emailView
    )

    // MARK: - Private

    private typealias UserEmailInputIdentifiers = UserEmailConfirmationAccessibilityIdentifiers
}

extension UserEmailConfirmationSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.emailView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func screenIsDismissed() -> Self {
        self.emailView.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func fillForm(email: String) -> Self {
        self.emailField
            .yreEnsureExists()
            .yreTap()
            .yreTypeText(email)
        return self
    }

    @discardableResult
    func formIsValid() -> Self {
        self.emailSubmitButton
            .yreEnsureEnabledWithTimeout()
        return self
    }

    @discardableResult
    func tapOnSubmit() -> Self {
        self.emailSubmitButton
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        self.closeButton
            .yreTap()
        return self
    }
}
