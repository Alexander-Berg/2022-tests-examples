//
//  AdViewPickerScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 4/20/21.
//

import XCTest
import Snapshots

class AdViewPickerScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var standardActivationButton = find(by: "standard_activation_button").firstMatch
}
