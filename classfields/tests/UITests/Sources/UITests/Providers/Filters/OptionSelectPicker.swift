import XCTest

final class OptionSelectPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case picker
        case resetButton
        case doneButton
        case option(String)
    }

    static let rootElementID = "OptionSelectPicker"
    static let rootElementName = "Мультивыбор"

    func identifier(of element: Element) -> String {
        switch element {
        case .picker:
            return "modal.picker"

        case .resetButton:
            return "modal_base.btn.left"

        case .doneButton:
            return "modal_base.btn.right"

        case .option(let id):
            return id
        }
    }
    
    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

