import XCTest
import Snapshots

typealias SaleListScreen_ = SaleCardListSteps

extension SaleListScreen_: UIRootedElementProvider {
    enum Element {
        case offerCell(OfferIdentifier, OfferPosition = .body)
        case addToSavedSearchIcon
        case savedSearchIcon
        case okButton // @kvld: элемент, который не относится к листингу, нужно отрефакторить
        case searchParams
        case geoRadiusBubbles
        case creditBanner
        case electrocarsBanner
        case searchSubscriptionButton
    }

    static let rootElementID = "sale_card_list"
    static let rootElementName = "Листинг"

    func identifier(of element: Element) -> String {
        switch element {
        case let .offerCell(offer, .title):
            return "offer_title_\(offer.id)"

        case let .offerCell(offer, .gallery):
            return "offer_image_\(offer.id)"

        case let .offerCell(offer, .body):
            return "offer_\(offer.id)"

        case let .offerCell(offer, .footer):
            return "separator_medium_offer_\(offer.id)"

        case .addToSavedSearchIcon:
            return "icn save search"

        case .savedSearchIcon:
            return "icn saved search"

        case .okButton:
            return "ОК"

        case .searchParams:
            return "filter_allParameters"

        case .geoRadiusBubbles:
            return "geoRadiusBubbles"

        case .creditBanner:
            return "preliminary_credit_banner"

        case .electrocarsBanner:
            return "electrocarsBanner"

        case .searchSubscriptionButton:
            return "search_subscription_button"
        }
    }
}
