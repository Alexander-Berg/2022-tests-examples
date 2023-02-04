import XCTest

final class ActivateSaleLaterPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case datePicker = "date_picker"
        case confirmButton = "confirm_button"
    }

    static let rootElementID = "ActivateSaleLaterPopup"
    static let rootElementName = "Попап после нажатия на Продам позже"

    func find(element: Element) -> XCUIElement {
        switch element {
        case .confirmButton:
            return rootElement
        case .datePicker:
            return rootElement
                .descendants(matching: .datePicker)
                .matching(identifier: "date_picker")
                .firstMatch
        }
    }
}
