//
//  WatchedOffersHistoryScreen.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 30.01.2021.
//

import XCTest
import Snapshots

class WatchedOffersHistoryScreen: BaseScreen, Scrollable {
    var scrollableElement: XCUIElement {
        find(by: "history_offers").firstMatch
    }
}

class WatchedOfferScreen: BaseScreen { }
