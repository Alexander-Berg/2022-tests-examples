import XCTest

final class ContextTooltipMenu: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case copy = "Скопировать"
    }

    static let rootElementID = ""
    static let rootElementName = "Контекстное меню текста (скопировать/вставить/...)"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        app.descendants(matching: .menu).firstMatch
    }
}
