import XCTest

final class AuctionSmallClaimCell: BaseSteps, UIElementProvider {
    enum Element: String {
        case submit = "submit_button"
        case more = "more_button"
        case close = "close_button"
        case priceRange = "price_range"
    }
}
