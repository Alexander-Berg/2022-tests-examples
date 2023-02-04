//
//  WatchedHistoryOffersSteps.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 30.01.2021.
//

import Foundation
import XCTest
import Snapshots

final class WatchedOffersHistorySteps: BaseSteps {
    func onScreen() -> WatchedOffersHistoryScreen {
        return baseScreen.on(screen: WatchedOffersHistoryScreen.self)
    }

    func offer(at index: Int) -> WatchedOfferSteps {
        let element = onScreen().scrollToCell(at: 0, swipeDirection: .left)
        onScreen().scrollableElement.scrollTo(element: element, swipeDirection: .left)

        return WatchedOfferSteps(context: context)
    }
}

class WatchedOfferSteps: BaseSteps {
    func onScreen() -> WatchedOfferScreen {
        return baseScreen.on(screen: WatchedOfferScreen.self)
    }
}
