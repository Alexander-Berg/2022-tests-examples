//
//  GarageAllPromosScreen.swift
//  UITests
//
//  Created by Igor Shamrin on 24.03.2022.
//

import XCTest

final class GarageAllPromosScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "garage_all_promos_screen"
    static let rootElementName = "Экран все акции"

    enum BottomButton: String {
        case goBack = "Вернуться в гараж"
    }

    enum Element {
        case bigPromo(title: String)
        case commonPromo(title: String)
        case bottomButton(BottomButton)
        case backButton
        case crossButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .bigPromo(title: let title):
            return "all_promos_big_promo" + title
        case .commonPromo(title: let title):
            return "all_promos_common_promo" + title
        case .bottomButton(let button):
            return button.rawValue
        case .backButton:
            return "backButton"
        case .crossButton:
            return "nav_close_button"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

