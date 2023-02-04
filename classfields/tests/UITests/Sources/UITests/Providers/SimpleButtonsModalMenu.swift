//
//  SimpleButtonsModalMenu.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/29/21.
//

import XCTest
import Snapshots

final class SimpleButtonsModalMenu: BaseSteps, UIRootedElementProvider {
    enum Element {
        case button(_ title: String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .button(title):
            return "SimpleButtonsModalMenu_button_\(title)"
        }
    }

    static let rootElementID = "SimpleButtonsModalMenu"
    static let rootElementName = "Анимированное модальное окно с кнопками"
}
