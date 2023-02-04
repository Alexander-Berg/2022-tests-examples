import Foundation

final class SafeDealSellerConfirmationPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case title = "title"
        case description = "description"
        case contact = "contact_button"
        case reject = "reject_button"
        case approve = "approve_button"
        case list = "list_button"
        case agreement = "agreement"
        case close = "close_button"
    }

    static let rootElementName = "Попап подтверждения запроса на БС"
    static let rootElementID = "safe_deal_seller_confirmation_popup"
}
