import FormKit
import MarketUI
import XCTest

class NoticeInfoPage: PageObject {

    // MARK: - Public

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: NoticeAccessibility.text)
            .element(boundBy: 0)
    }

    var image: XCUIElement {
        element
            .images
            .matching(identifier: NoticeAccessibility.image)
            .element(boundBy: 0)
    }

}
