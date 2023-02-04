//
//  GarageSearchScreen.swift
//  UITests
//
//  Created by Igor Shamrin on 29.11.2021.
//

import XCTest

final class GarageSearchScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "garage_vin_search_view"
    static let rootElementName = "Экран поиска по VIN/ГРЗ в гараже"

    enum Element {
        case vinInputField
        case govNumberInputField
        case selectedRegion
        case cameraButton
        case hintLabel
        case bottomButton(BottomButton)
    }

    enum BottomButton: String {
        case search = "Найти"
        case addManually = "Добавить вручную"
        case addByVin = "Добавить по VIN"
        case addToGarage = "Добавить в гараж"
        case goToGarage = "Перейти в гараж"
        case retry = "Попробовать ещё раз"
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .vinInputField:
            return "app.pickers.vin"
        case .govNumberInputField:
            return "app.views.gov_number"
        case .selectedRegion:
            return "garage_selected_region"
        case .cameraButton:
            return "app.pickers.vin.cameraButton"
        case .hintLabel:
            return "garage_search_hint_label"
        case .bottomButton(let button):
            return button.rawValue
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
