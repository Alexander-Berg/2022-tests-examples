import XCTest

final class ActivityListViewPage: PageObject {

    static var current: ActivityListViewPage {
        let element = XCUIApplication().otherElements["ActivityListView"]
        return ActivityListViewPage(element: element)
    }

    var collectionView: XCUIElement {
        element
            .collectionViews
            .firstMatch
    }

    /// У кнопок в шер диалоге нет идентификаторов, поэтому доставать можно только по лейблу
    /// который локализуется. Не страшно в нашем случае — тесты всегда запускаются в русской локали.
    var copyButton: XCUIElement {
        collectionView.buttons["Скопировать"]
    }
}
