import XCTest

final class SafeDealSellingPricePopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case price = "price"
        case submit = "submit_button"
        case agreement = "agreement_link"
    }

    static let rootElementName = "Попап ввода цены запроса на БС"
    static let rootElementID = "safe_deal_selling_price_popup"
}
