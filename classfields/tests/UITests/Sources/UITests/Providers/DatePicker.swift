import XCTest

typealias DatePickerScreen_ = DatePickerSteps

extension DatePickerScreen_: UIRootedElementProvider {
    static let rootElementID = "date_picker"
    static let rootElementName = "Пикер даты"

    enum Element: String {
        case resetButton = "modal_base.btn.left"
        case doneButton = "modal_base.btn.right"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
