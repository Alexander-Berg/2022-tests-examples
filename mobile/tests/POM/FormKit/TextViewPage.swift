import FormKit
import MarketUI
import XCTest

class TextViewPage: PageObject {

    var textView: XCUIElement {
        element.textViews
            .matching(identifier: TextViewAccessibility.textView).element
    }

    var placeholder: XCUIElement {
        element.staticTexts.matching(identifier: TextViewAccessibility.placeholder).element
    }

    func typeText(_ text: String) {
        textView.tap()
        textView.typeText(text)
    }
}
