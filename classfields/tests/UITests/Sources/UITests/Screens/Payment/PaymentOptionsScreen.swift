//
//  PaymentOptionsScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 4/13/21.
//

import XCTest
import Snapshots

final class PaymentOptionsScreen: ModalScreen {
    lazy var paymentOptionsScreen = find(by: "app.screen.payment_options").firstMatch
    lazy var packagesContainer = find(by: "app.payments.view.packages_container").firstMatch
    lazy var purchaseButton = find(by: "purchase_button").firstMatch
    lazy var activityHUD = find(by: "ActivityHUD").firstMatch

    lazy var enterPromocodeCell = app.collectionViews.firstMatch.cells.matching(identifier: "Ввести промокод").firstMatch
}
