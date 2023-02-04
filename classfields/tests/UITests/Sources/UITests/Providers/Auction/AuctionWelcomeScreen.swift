import XCTest

final class AuctionWelcomeScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case skipButton = "Пропустить"
        case nextButton = "submit_button"
        case howItWorksLink = "how_it_works"
        case agreementText = "agreement_text"
    }

    static var rootElementName = "Экран информации об аукционе"
    static var rootElementID = "auction_welcome_screen"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
