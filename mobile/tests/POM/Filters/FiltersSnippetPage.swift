import UIUtils
import XCTest

class FiltersSnippetPage: PageObject, FilterEntryPoint {

    var checkBox: XCUIElement {
        element
            .images
            .firstMatch
    }

    var name: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FiltersSnippetAccessibility.name)
            .firstMatch
    }

    var value: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FiltersSnippetAccessibility.value)
            .firstMatch
    }

    var switchElem: XCUIElement {
        element
            .switches
            .matching(identifier: FiltersSnippetAccessibility.switchElem)
            .firstMatch
    }

}
