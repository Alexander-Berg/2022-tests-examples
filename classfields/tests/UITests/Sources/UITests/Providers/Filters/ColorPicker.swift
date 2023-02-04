import XCTest

final class ColorPicker: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case picker = "modal.picker"
        case resetButton = "modal_base.btn.left"
        case doneButton = "modal_base.btn.right"
    }

    static let rootElementID = "RangePickerViewController"
    static let rootElementName = "Выбор от-до"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
