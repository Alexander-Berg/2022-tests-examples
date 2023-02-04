//
//  Created by Alexey Aleshkov on 21/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

extension SystemDialogs {
    static func makeLocationActivity(_ testCase: XCTestCase) -> SystemDialogActivity<LocationDialogHandler> {
        return .init(testCase: testCase, handler: .init())
    }
}

final class LocationDialogHandler: SystemDialogActivityHandlerProtocol {
    // MARK: -

    enum Button {
        case allow
        case disallow
    }

    var button: Button?

    let name: String = "Location system alert"

    func handleDialog(_ element: XCUIElement) -> Bool {
        let buttonCase = YREUnwrap(self.button)

        let titleLabel = element.label

        if let localization = Localization.localization(for: titleLabel) {
            let button: XCUIElement
            switch buttonCase {
                case .allow:
                    button = element.buttons[localization.allowButtonLabel]
                case .disallow:
                    button = element.buttons[localization.disallowButtonLabel]
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
        let allowButtonLabel: String
        let disallowButtonLabel: String

        static let english = Localization(
            // "Allow “Недвижимость” to access your location while you are using the app?"
            alertTitleLabelParts: ["access", "location"],
            allowButtonLabel: "Allow",
            disallowButtonLabel: "Don’t Allow"
        )

        static let russian = Localization(
            // "Разрешить доступ к Вашим геоданным программе «Недвижимость», пока Вы используете ее?"
            alertTitleLabelParts: ["доступ", "геоданны"],
            allowButtonLabel: "Разрешить",
            disallowButtonLabel: "Запретить"
        )

        static func localization(for label: String) -> Localization? {
            let locs: [Localization] = [.english, .russian]
            let result = locs.first(where: { $0.alertTitleLabelParts.allSatisfy({ label.localizedCaseInsensitiveContains($0) }) })
            return result
        }
    }
}
