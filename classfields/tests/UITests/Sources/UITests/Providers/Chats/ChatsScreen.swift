import XCTest

typealias ChatsScreen_ = ChatsSteps

extension ChatsScreen_: UIRootedElementProvider {
    enum Element {
        case chatRoom(id: String)
    }

    static let rootElementID = "chats"
    static let rootElementName = "Список чатов"

    func identifier(of element: Element) -> String {
        switch element {
        case let .chatRoom(id: id):
            return "chat_\(id)"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
