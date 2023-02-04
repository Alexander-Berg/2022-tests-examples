//
//  PromocodeBottomsheetScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 3/25/21.
//

import XCTest
import Snapshots

final class PromocodeBottomsheetScreen: ModalScreen {
    lazy var activateButton = findAll(.button)["activate_button"].firstMatch
    lazy var clearButton = app.cells["promocode_input"].textFields.buttons["clear icon"].firstMatch
    lazy var textField = app.cells["promocode_input"].textFields.firstMatch
}
