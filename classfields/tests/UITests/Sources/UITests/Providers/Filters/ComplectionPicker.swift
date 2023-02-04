import XCTest

final class ComplectationPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case item(String)
        case rootItem(String)
        case resetButton
        case doneButton
    }

    static let rootElementID = "ExtrasFilterPickerView"
    static let rootElementName = "Выбор опций"

    func identifier(of element: Element) -> String {
        switch element {
        case let .item(id):
            return id
        case let .rootItem(id):
            return ".root.\(id)"
        case .resetButton:
            return "Сбросить"
        case .doneButton:
            return "Готово"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case let .item(id):
            return "Элемент: \(id)"
        case let .rootItem(id):
            return "Корневой элемент: \(id)"
        case .resetButton:
            return "Сбросить"
        case .doneButton:
            return "Готово"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

