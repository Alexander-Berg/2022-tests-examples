import UIUtils
import XCTest

final class HotlinkPage: PageObject {

    var textLabel: XCUIElement {
        element.staticTexts.matching(identifier: DetailsVerticalAccessibility.label).firstMatch
    }

}
