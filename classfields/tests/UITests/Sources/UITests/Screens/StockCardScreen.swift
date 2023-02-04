//
//  StockCardScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 7/20/20.
//

import XCTest
import Snapshots

class StockCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    enum FilterType: String {
        case all = "Параметры"
        case complectation = "quickFiltersComplectation"
        case engine = "quickFiltersEngine"
        case transmission = "quickFiltersTransmission"
        case gear = "quickFiltersGear"
        case color = "quickFiltersColor"
        case inStock = "quickFiltersInStock"
    }

    var filtersScrollable: XCUIElement {
        find(by: "quickFilters").firstMatch
    }

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    lazy var complectationPickerButton = find(by: "Комплектация").firstMatch
    lazy var aboutModelButton = find(by: "О модели").firstMatch

    lazy var emptyResultsPlaceholder = find(by: "empty_result_cell").firstMatch
    lazy var emptyResultsResetButton = find(by: "Очистить запрос").firstMatch

    lazy var paramsView = find(by: "light_form_0").firstMatch
    lazy var paramsChipsView = find(by: "light_form_params").firstMatch

    func stockOfferBody(for offerId: String) -> XCUIElement {
        return find(by: "offer_\(offerId)").firstMatch
    }

    func filterButton(_ type: FilterType) -> XCUIElement {
        return find(by: type.rawValue).firstMatch
    }

    func offerTitle(id: String) -> XCUIElement {
        return find(by: "stock_offer_title_\(id)").firstMatch
    }
}
