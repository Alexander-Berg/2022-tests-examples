import XCTest

final class AuctionClaimScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case claimButton = "Записаться на осмотр"
        case agreementText = "agreement_text"
        case agreementCheckmark = "agreement_checkmark"
    }

    static var rootElementName = "Экран заявки на аукцион"
    static var rootElementID = "auction_claim_screen"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
