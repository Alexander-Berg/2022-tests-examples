import XCTest

final class AutoruOnlyPopupScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "autoru_only_popup"
    static let rootElementName = "Попап с информацией про бейдж Только на Авто.ру"

    enum Element {
        case autoruOnlyDisclaimer
        case closeButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .autoruOnlyDisclaimer:
            return "autoru_only_disclaimer"

        case .closeButton:
            return "dismiss_modal_button"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        let host = app.descendants(matching: .any).matching(identifier: "ModalViewControllerHost").firstMatch
        assertRootExists(in: host)
        return host
    }
}
