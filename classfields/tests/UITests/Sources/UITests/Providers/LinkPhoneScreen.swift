import XCTest

final class LinkPhoneScreen: BaseSteps, UIRootedElementProvider {
    static var rootElementID = "login_view"
    static var rootElementName = "Экран привязки телефона"

    enum Element: String {
        case phoneInput = "phoneInput"
        case closeButton = "closeButton"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
