import XCTest

final class WizardReviewScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "wizard_review_container_view"
    static let rootElementName = "Контейнер визарда"

    enum Element: String {
        case closeButton = "close_wizard_review_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
