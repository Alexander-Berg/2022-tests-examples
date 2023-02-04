import XCTest
import Snapshots

final class ChatScreen: BaseScreen, Scrollable, NavigationControllerContent {
    var scrollableElement: XCUIElement {
        return self.collectionView
    }

    lazy var collectionView = findAll(.collectionView).firstMatch
    lazy var presets = find(by: "chat_preset_item").firstMatch
}
