import XCTest

class SpecsPage: PageObject {

    static var current: SpecsPage {
        let specsEl = XCUIApplication().otherElements[SpecsAccessibility.root]
        return SpecsPage(element: specsEl)
    }

    /// Блоки с заголовками групп характеристик
    var titlesBlocks: [XCUIElement] {
        element
            .cells
            .matching(identifier: SpecsAccessibility.specCellTitle)
            .allElementsBoundByIndex
    }

    var specsCellEntries: [XCUIElement] {
        element
            .cells
            .matching(identifier: SpecsAccessibility.specCellEntry)
            .allElementsBoundByIndex
    }

    var qnaEntryView: XCUIElement {
        element
            .otherElements
            .matching(identifier: QnAAccessibility.entryPointView)
            .firstMatch
    }

    /// NavigationBar на экране характеристик
    var navigationBar: NavigationBarPage {
        NavigationBarPage(element: NavigationBarPage.current.element)
    }
}
