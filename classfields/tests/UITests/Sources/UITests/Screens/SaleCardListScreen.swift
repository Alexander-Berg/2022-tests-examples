//
//  SaleCardListScreen.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/17/19.
//

import XCTest
import Snapshots

class SaleCardListScreen: BaseScreen, Scrollable, NavigationControllerContent {
    lazy var addToFavoritesButton = app.buttons["icn save search"]
    lazy var sortingInfoContainer = find(by: "sorting_counter").firstMatch
    lazy var bestPriceBanner = find(by: "view.new_car_request.container").firstMatch
    lazy var bestPriceBannerButton = find(by: "Получить лучшую цену").firstMatch
    lazy var filterButton = find(by: "filter_allParameters").firstMatch
    lazy var confirmAlertText = findContainedText(by: "Поиск сохранён.").firstMatch
    lazy var confirmAlertOK = find(by: "ОК").firstMatch

    func offersCardTitle(for offerId: String) -> XCUIElement {
        return find(by: "offer_title_\(offerId)").firstMatch
    }

    func offerBody(for offerId: String) -> XCUIElement {
        return find(by: "offer_\(offerId)").firstMatch
    }

    func offerFooter(for offerId: String) -> XCUIElement {
        return find(by: "separator_medium_offer_\(offerId)").firstMatch
    }

    func offerFavButton(forId id: String) -> XCUIElement {
        return find(by: "fav_offer_\(id)").firstMatch
    }

    func offerAutoRuBadge() -> XCUIElement {
        return find(by: "Только на Авто.ру").firstMatch
    }

    func reportBadge() -> XCUIElement {
        return find(by: "Отчёт ПроАвто").firstMatch
    }

    func stockOfferTitle(for offerId: String) -> XCUIElement {
        return find(by: "stock_offer_title_\(offerId)").firstMatch
    }

    func stockOfferBody(for offerId: String) -> XCUIElement {
        return find(by: "stock_offer_\(offerId)").firstMatch
    }

    func stockOfferRequest(for offerId: String) -> XCUIElement {
        return find(by: "stock_offer_request_\(offerId)").firstMatch
    }

    func segmentedControlAt(index: Int) -> XCUIElement {
        return find(by: "segmentControlSegmentLabel_\(index)").firstMatch
    }

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }
}
