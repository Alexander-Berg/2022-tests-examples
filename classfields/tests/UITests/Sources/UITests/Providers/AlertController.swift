//
//  UIAlertController.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/30/21.
//

import XCTest
import Snapshots

final class AlertController: BaseSteps, UIRootedElementProvider {
    enum Element {
        case button(_ title: String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .button(title):
            return title
        }
    }

    static let rootElementID = "UIAlertController"
    static let rootElementName = "Простой системный алерт"
}
