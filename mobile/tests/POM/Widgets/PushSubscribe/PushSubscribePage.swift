import XCTest

final class PushSubscribe: PageObject {

    // MARK: - Properties

    var title: XCUIElement {
        element.staticTexts.matching(identifier: PushSubscribeAccessibility.titleInfo).firstMatch
    }

    var actionButton: XCUIElement {
        element.buttons.matching(identifier: PushSubscribeAccessibility.actionButton).firstMatch
    }

    var icon: XCUIElement {
        element.images.matching(identifier: PushSubscribeAccessibility.iconImage).firstMatch
    }

    var closeButton: XCUIElement {
        element.buttons.matching(identifier: PushSubscribeAccessibility.closeButton).firstMatch
    }

}
