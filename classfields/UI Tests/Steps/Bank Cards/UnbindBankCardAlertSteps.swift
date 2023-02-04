//
//  Created by Alexey Aleshkov on 27/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.BankCardsListAccessibilityIdentifiers

final class UnbindBankCardAlertSteps: AnyAlertSteps {
    convenience init() {
        self.init(
            elementType: .sheet,
            alertID: BankCardsIdentifiers.unbindAlertViewIdentifier
        )
    }

    @discardableResult
    func tapOnSubmit() -> Self {
        return self.tapOnButton(withID: Self.submitButtonID)
    }

    @discardableResult
    func tapOnCancel() -> Self {
        return self.tapOnButton(withID: Self.cancelButtonID)
    }

    private typealias BankCardsIdentifiers = BankCardsListAccessibilityIdentifiers
    private static let submitButtonID = BankCardsIdentifiers.unbindAlertSubmitTitle
    private static let cancelButtonID = BankCardsIdentifiers.unbindAlertCancelTitle
}
