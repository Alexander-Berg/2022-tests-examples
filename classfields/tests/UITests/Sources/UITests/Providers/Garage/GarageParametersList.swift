import XCTest

final class GarageParametersListScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "garage_params_list"
    static let rootElementName = "Список параметров карточки гаража"

    enum Element: String {
        case closeButton = "dismiss_modal_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}

