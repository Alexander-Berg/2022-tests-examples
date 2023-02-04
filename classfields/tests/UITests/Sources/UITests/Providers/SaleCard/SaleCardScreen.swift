import XCTest

typealias SaleCardScreen_ = SaleCardSteps

extension SaleCardScreen_: UIRootedElementProvider {
    enum Element: String {
        case bottomButtonsContainer = "bottom_buttons_container"
        case images
        case moreButton = "more_button"
        case complainButton = "btn.sale_card.complaint"
        case safeDeal = "safe_deal"
        case safeDealStatus = "safe_deal_status"
        case sellerInfo = "seller_info"
        case callBackAction = "call_back_action"
        case tradeInButton = "trade_in_button"
        case tradeInButtonInfo = "trade_in_info_button"
        case autoruOnlyBadge = "auto_ru_only"
        case actionButtons = "action_buttons"
        case userNote = "user_note"
        case purchaseReportButton = "report_purchase_button"
        case fullReportButton = "Смотреть полный отчёт"
        case freeReportButton = "Смотреть бесплатный отчёт"
        case saleDescription = "sale_card_description"
        case сharacteristicCell = "CharacteristicBlock"
        case carReportPreview = "backend_layout_cell"
        case priceHistoryButton = "sale_card.header.btn.price_hist"
        case deliveryBanner = "delivery_banner"
        case dealerListingButton = "dealer_listing_action"
        case publicProfileInfo = "public_profile_info"
        case publicProfileSales = "public_profile_sales"
    }

    static let rootElementID: String = "sale_card"
    static let rootElementName: String = "Карточка оффера"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
