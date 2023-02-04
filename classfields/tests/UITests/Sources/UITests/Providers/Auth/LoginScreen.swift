import XCTest
import Snapshots

final class LoginScreen_: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case phoneInput = "phoneInput"
        case closeButton = "closeButton"
    }

    static let rootElementID = "login_view"
    static let rootElementName = "Форма логина"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
