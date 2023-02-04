//
//  SharkSelectOffer.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.12.2020.
//

import XCTest
import Snapshots

class SharkSelectOfferSteps: BaseSteps {
    func selectFromListing() -> SaleCardListSteps {
        onMainScreen().find(by: "Я найду другой").firstMatch.tap()
        return SaleCardListSteps(context: context)
    }
}
