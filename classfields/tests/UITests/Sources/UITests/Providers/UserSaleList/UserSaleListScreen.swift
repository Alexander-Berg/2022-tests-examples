import XCTest
import Snapshots

typealias UserSaleListScreen = OffersSteps

extension UserSaleListScreen: UIRootedElementProvider {
    enum Element {
        case profileButton
        case logInLabel
        case placeFreeLabel
        case offer(id: String)
        case enableApp2App(offerID: String)
        case draftSnippetActions(id: String)
        case activeSnippetActions(id: String)
        case garagePromoBanner
        case auctionSnippetInfo(id: String)
        case auctionSnippetActions(id: String)
        case vasHeader
        case offerMainButton
        case vas(id: String, name: String)
        case expandButton
        case auctionSmallClaim(offerID: String)
        case publicProfileTooltipPopup
        case publicProfilePromoBanner
    }

    static let rootElementID: String = "UserSaleListViewController"
    static let rootElementName: String = "Объявления юзера"

    func identifier(of element: Element) -> String {
        switch element {
        case .profileButton:
            return "user_profile_button"

        case .logInLabel:
            return "Войти"

        case .placeFreeLabel:
            return "Разместить бесплатно"

        case let .offer(id):
            return "offer_\(id)_details"

        case let .enableApp2App(offerID: offerID):
            return "enableApp2App_\(offerID)"

        case let .draftSnippetActions(id):
            return "offer_\(id)_archiveSnippetActions"

        case let .activeSnippetActions(id):
            return "offer_\(id)_activeSnippetActions"

        case .garagePromoBanner:
            return "В гараж"

        case .auctionSnippetInfo(let id):
            return "auction_info_\(id)"

        case .auctionSnippetActions(let id):
            return "auction_\(id)_actions"
        
        case .vasHeader:
            return "offer_vas_header"

        case .offerMainButton:
            return "offer_main_button"

        case let .vas(id, name):
            return "offer_\(id)_\(name)_package"

        case .expandButton:
            return "offer_vas_expand_button"

        case .auctionSmallClaim(let offerID):
            return "auction_small_claim_\(offerID)"

        case .publicProfileTooltipPopup:
            return "public_profile_tooltip"

        case .publicProfilePromoBanner:
            return "public_profile_banner"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
