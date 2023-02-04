//
//  PushNotificationsDialogHandler.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 19.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

extension SystemDialogs {
    static func makePushNotificationsActivity(
        _ testCase: XCTestCase
    ) -> SystemDialogActivity<PushNotificationsDialogHandler> {
        return .init(testCase: testCase, handler: .init())
    }
}

final class PushNotificationsDialogHandler: SystemDialogActivityHandlerProtocol {
    enum Button {
        case allow
        case disallow
    }

    var button: Button?

    let name: String = "Push notifications system alert"

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
            // “Недвижимость” would like to send you notifications"
            alertTitleLabelParts: ["send", "notification"],
            allowButtonLabel: "Allow",
            disallowButtonLabel: "Don’t Allow"
        )

        static let russian = Localization(
            // "Приложение «Недвижимость» запрашивает разрешение на отправку вам уведомлений"
            alertTitleLabelParts: ["отправ", "уведомл"],
            allowButtonLabel: "Разрешить",
            disallowButtonLabel: "Не разрешать"
        )

        static func localization(for label: String) -> Localization? {
            let locs: [Localization] = [.english, .russian]
            let result = locs.first(where: { $0.alertTitleLabelParts.allSatisfy({ label.localizedCaseInsensitiveContains($0) }) })
            return result
        }
    }
}
