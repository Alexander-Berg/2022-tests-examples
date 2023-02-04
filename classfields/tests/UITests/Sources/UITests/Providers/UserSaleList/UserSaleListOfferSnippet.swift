import XCTest

final class UserSaleListOfferVASCell: BaseSteps, UIElementProvider {
    enum Element: String {
        case title = "vas_title"
        case text = "vas_text"
        case price = "vas_price"
    }
}

final class UserSaleListOfferVASHeaderCell: BaseSteps, UIElementProvider {
    enum Element: String {
        case title = "header_title"
        case text = "header_text"
    }
}
