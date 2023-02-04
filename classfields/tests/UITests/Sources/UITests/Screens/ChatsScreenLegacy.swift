import XCTest

final class ChatsScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    func chatCell(index: Int) -> XCUIElement {
        return app.collectionViews.element(boundBy: 0).cells.element(boundBy: index).firstMatch
    }

    func chatCell(id: String) -> XCUIElement {
        return find(by: "chat_\(id)").firstMatch
    }
}
