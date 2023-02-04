//
//  Created by Alexey Aleshkov on 21/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

extension SystemDialogs {
    static func makePasswordActivity(_ testCase: XCTestCase) -> SystemDialogActivity<PasswordDialogHandler> {
        return .init(testCase: testCase, handler: .init())
    }
}

final class PasswordDialogHandler: SystemDialogActivityHandlerProtocol {
    // MARK: -

    enum Button {
        case allow
        case disallow
    }

    var button: Button?

    let name: String = "Password system alert"

    func handleDialog(_ element: XCUIElement) -> Bool {
        let buttonCase = YREUnwrap(self.button)

        let titleLabel = element.label
        if let localization = Localization.localization(for: titleLabel) {
            let button: XCUIElement
            switch buttonCase {
                case .allow:
                    button = element.buttons[localization.saveButtonLabel]
                case .disallow:
                    button = element.buttons[localization.cancelButtonLabel]
            }

            button
                .yreEnsureExists()
                .yreTap()

            return true
        }

        return false
    }

    private struct Localization {
        let alertTitleLabelParts: [String]
        let saveButtonLabel: String
        let cancelButtonLabel: String

        static let english = Localization(
            // "Would you like to save this password in your iCloud Keychain to use with apps and websites on all your devices?"
            alertTitleLabelParts: ["save", "password", "keychain"],
            saveButtonLabel: "Save Password",
            cancelButtonLabel: "Don’t Allow"
        )

        static let russian = Localization(
            // "Хотите сохранить этот пароль в своей связке ключей iCloud для его использования с программами и веб‑сайтами на всех Ваших устройствах?"
            alertTitleLabelParts: ["сохран", "парол", "связк", "ключ"],
            saveButtonLabel: "Сохранить пароль",
            cancelButtonLabel: "Запретить"
        )

        static func localization(for label: String) -> Localization? {
            let locs: [Localization] = [.english, .russian]
            let result = locs.first(where: { $0.alertTitleLabelParts.allSatisfy({ label.localizedCaseInsensitiveContains($0) }) })
            return result
        }
    }
}
