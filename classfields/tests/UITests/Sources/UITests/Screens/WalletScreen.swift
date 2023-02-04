//
//  WalletScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 4/9/21.
//

import XCTest
import Snapshots

final class WalletScreen: BaseScreen, Scrollable {
    enum Segment: Int, CustomStringConvertible {
        case balance = 0
        case cards = 1
        case promocodes = 2

        var description: String {
            switch self {
            case .balance:
                return "Баланс"
            case .cards:
                return "Привязанные карты"
            case .promocodes:
                return "Промокоды"
            }
        }
    }

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    lazy var segmentControl = find(by: "segmentControl").firstMatch
    lazy var promocodeInput = find(by: "activate_promocode_input").firstMatch
    lazy var promocodeClearButton = app.cells["activate_promocode_input"].textFields.buttons["clear icon"].firstMatch
    lazy var promocodeTextField = app.cells["activate_promocode_input"].textFields.firstMatch
    lazy var promocodeActivateButton = find(by: "activate_promocode_button").firstMatch
    lazy var refundWalletButton = find(by: "refund_wallet_button").firstMatch

    func segmentControl(at tab: Segment) -> XCUIElement {
        return find(by: "segmentControlSegment_\(tab.rawValue)").firstMatch
    }

    func promocodeCell(idPrefix: String) -> XCUIElement {
        return app.collectionViews
            .firstMatch
            .cells
            .withIdentifierPrefix(idPrefix)
            .firstMatch
    }
}
