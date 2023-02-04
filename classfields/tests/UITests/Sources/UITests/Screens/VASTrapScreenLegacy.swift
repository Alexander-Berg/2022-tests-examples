//
//  VASTrapScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 4/19/21.
//

import XCTest
import Snapshots

class VASTrapScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var purchaseVASButton = find(by: "purchase_vas_button").firstMatch
}
