import XCTest

final class AuctionSuccessClaimScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case closeButton = "Закрыть"
        case showClaimsButton = "Посмотреть статус заявки"
    }

    static var rootElementName = "Экран после успешной подачи заявки аукциона"
    static var rootElementID = "auction_success_claim_screen"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
