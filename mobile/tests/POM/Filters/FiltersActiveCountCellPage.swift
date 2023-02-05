import UIUtils
import XCTest

class FiltersActiveCountCellPage: PageObject, FilterEntryPoint {

    var countLabel: XCUIElement {
        element.staticTexts.matching(identifier: FilterActiveCountCellAccessibility.countLabel).firstMatch
    }

    var dropButton: XCUIElement {
        element.buttons.matching(identifier: FilterActiveCountCellAccessibility.dropButton).firstMatch
    }

}
