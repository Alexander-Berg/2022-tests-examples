import XCTest

final class MarkModelPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case mark(String)
        case model(String)
        case generation(Int)
    }

    static let rootElementID = "container_view"
    static let rootElementName = "Пикер марки-модели"

    func identifier(of element: Element) -> String {
        switch element {
        case .mark(let id):
            return id
        case .model(let id):
            return id
        case .generation(let id):
            return "\(id)"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
