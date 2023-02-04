import Foundation
import XCTest
import Snapshots

final class GarageCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    lazy var scrollableElement = app.collectionViews.firstMatch

    lazy var editButton = find(by: "Изменить").firstMatch
    lazy var priceStatsBlock = find(by: "cell_price_stats").firstMatch
    lazy var recallsButton = find(by: "header_recalls").firstMatch
    lazy var priceStatsButton = find(by: "header_price_stats").firstMatch
    lazy var priceRow = find(by: "price_header").firstMatch
    lazy var cheapeningBlock = find(by: "cell_cheapening_graph").firstMatch
    lazy var reportBlock = find(by: "backend_layout_cell_garage_buy_report").firstMatch

    lazy var priceStatsBlockExchangeLink = find(by: "PriceStatsBottomBlock").firstMatch

    lazy var calculateCarPriceBanner = find(by: "Рассчитайте стоимость автомобиля").firstMatch
    lazy var buyReportButton = find(by: "report_purchase_button").firstMatch

    lazy var provenOwnerHeaderButton = find(by: "proven_owner_header").firstMatch

    func findText(_ text: String) -> XCUIElement { findContainedText(by: text).firstMatch }

    func photo(_ id: String) -> XCUIElement {
        find(by: id).firstMatch
    }

    enum AutoServicesBlockItem: String {
        case oils = "Масла и автохимия"
        case tyres = "Шины и диски"
        case general = "Запчасти"
    }
}

final class GarageCardPromoScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = app.collectionViews.firstMatch

    lazy var promoPopup = find(by: "ModalViewControllerHost").firstMatch
    lazy var actionButton = find(by: "promo_action_button").firstMatch
}
