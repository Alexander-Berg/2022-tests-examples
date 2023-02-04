//
//  DealerCardScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 17.07.2020.
//

import XCTest
import Snapshots

final class DealerCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    lazy var titleView = find(by: "NavBarTitle").firstMatch
    lazy var callNavBarView = find(by: "callNavBarView").firstMatch
    lazy var dealerSaveNavBarView = find(by: "dealerSaveNavBarView").firstMatch

    lazy var callButton = find(by: "callButton").firstMatch

    lazy var callBackButton = find(by: "callBackButton").firstMatch
    lazy var map = find(by: "map").firstMatch
    lazy var filtersButton = find(by: "ExpandingCounterButton").firstMatch
    lazy var descriptionTitle = findStaticText(by: "Предложения дилера").firstMatch
}
