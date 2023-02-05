import FormKit
import MarketUI
import XCTest

class TextLabelPage: PageObject {

    var textView: XCUIElement {
        element.textViews
            .matching(identifier: TextLabelAccessibility.textView).element
    }
}
