import XCTest
import Snapshots

final class CreditBannerPopup: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "preliminary_credit_banner"
    static let rootElementName = "Предварительная заявка на кредит"

    enum Element: String {
        case dismissButton = "dismiss_modal_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
