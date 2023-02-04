//
//  BestOffersScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 7/20/20.
//

import XCTest
import Snapshots

class BestOffersScreen: BaseScreen {
    func tradeInSwitcher() -> XCUIElement {
        return find(by: "trade_in_switcher").firstMatch
    }

    func creditSwitcher() -> XCUIElement {
        return find(by: "credit_switcher").firstMatch
    }

    func phone() -> XCUIElement {
        return find(by: "phone").firstMatch
    }

    func requestButton() -> XCUIElement {
        return find(by: "Отправить заявку").firstMatch
    }

    func dismissButton() -> XCUIElement {
        return find(by: "dismiss_modal_button").firstMatch
    }
}
