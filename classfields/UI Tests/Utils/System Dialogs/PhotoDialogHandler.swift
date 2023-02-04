//
//  PhotoDialogHandler.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 12.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

extension SystemDialogs {
    static func makePhotoActivity(
        _ testCase: XCTestCase
    ) -> SystemDialogActivity<PhotoDialogHandler> {
        return .init(testCase: testCase, handler: .init())
    }
}

final class PhotoDialogHandler: SystemDialogActivityHandlerProtocol {
    enum Button {
        case allow
        case disallow
    }

    var button: Button?

    let name: String = "Photo library system alert"

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
            // “Недвижимость” would like to access your photos"
            alertTitleLabelParts: ["access", "photos"],
            allowButtonLabel: "Allow Access to All Photos",
            disallowButtonLabel: "Don’t Allow"
        )

        static let russian = Localization(
            // "Приложение «Недвижимость» запрашивает доступ к фото"
            alertTitleLabelParts: ["доступ", "фото"],
            allowButtonLabel: "Разрешить доступ ко всем фото",
            disallowButtonLabel: "Запретить"
        )

        static func localization(for label: String) -> Localization? {
            let locs: [Localization] = [.english, .russian]
            let result = locs.first(where: { $0.alertTitleLabelParts.allSatisfy({ label.localizedCaseInsensitiveContains($0) }) })
            return result
        }
    }
}
