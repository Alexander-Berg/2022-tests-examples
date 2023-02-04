import XCTest

final class TextRangePicker: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case picker = "modal.picker"
        case doneButton = "modal_base.btn.right"
        case from = "from"
        case to = "to"
    }

    static let rootElementID = "TextRangePickerViewController"
    static let rootElementName = "Выбор от-до"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
