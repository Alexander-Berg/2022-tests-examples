import XCTest

final class UserSaleCardScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case header
        case imageStub
        case advantages
        case garagePromoBanner
        case auctionSmallClaim
        case expirationCounters
        case statistics
        case vas(String)
        case characteristics
        case damages
        case sellerInfo
        case sellerAddress
        case addPanoramaBanner
        case closeAddPanoramaBannerButton
        case autoRuOnlyBadge
        case shareButton
        case editButton
    }

    static let rootElementID: String = "UserSaleCardViewController"
    static let rootElementName: String = "Карточка оффера в ЛК"

    func identifier(of element: Element) -> String {
        switch element {
        case .header:
            return "header"

        case .imageStub:
            return "images_stub"

        case .advantages:
            return "advantages"

        case .garagePromoBanner:
            return "В гараж"

        case .auctionSmallClaim:
            return "auction_small_claim*"

        case .expirationCounters:
            return "expirationCounters"

        case .statistics:
            return "statistics"

        case let .vas(name):
            return "offer_\(name)"

        case .characteristics:
            return "details"

        case .damages:
            return "damages"

        case .sellerInfo:
            return "seller_info"

        case .sellerAddress:
            return "seller_address"

        case .addPanoramaBanner:
            return "AddPanoramaListBannerView"

        case .closeAddPanoramaBannerButton:
            return "closeAddPanoramaBannerButton"

        case .autoRuOnlyBadge:
            return "auto_ru_only"

        case .shareButton:
            return "share_button"

        case .editButton:
            return "editOfferNavBarItem"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
