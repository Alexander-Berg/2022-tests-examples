//
//  PhonePickerScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 7/29/20.
//

import XCTest
import Snapshots

class PhonePickerScreen: BaseScreen, NavigationControllerContent {
    lazy var completeButton = find(by: "Готово").firstMatch

    func phone(_ phone: String) -> XCUIElement {
        return find(by: phone).firstMatch
    }
}
