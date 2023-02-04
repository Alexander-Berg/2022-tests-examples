//
//  ModalScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 5/6/20.
//

import XCTest
import Snapshots

class ModalScreen: BaseScreen {
    func dismissButton() -> XCUIElement {
        return find(by: "dismiss_modal_button").firstMatch
    }
}

class LayoutPopUpModalScreen: ModalScreen {
    override func snapshot() -> UIImage {
        return find(by: "app.controller.LayoutPopUpViewController").firstMatch
            .waitAndScreenshot().image
    }
}
