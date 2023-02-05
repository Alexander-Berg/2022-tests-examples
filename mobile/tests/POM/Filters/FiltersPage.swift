import UIUtils
import XCTest

class FiltersPage: PageObject {

    // MARK: - Nested Types

    class CategoryPage: PageObject {
        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: FiltersAccessibility.categoryTitle)
                .firstMatch
        }

        var value: XCUIElement {
            element
                .staticTexts
                .matching(identifier: FiltersAccessibility.categoryValue)
                .firstMatch
        }
    }

    class DoneButtonPage: PageObject, FeedEntryPoint {}

    // MARK: - Properties

    var closeButton: XCUIElement {
        XCUIApplication()
            .buttons
            .matching(identifier: FiltersAccessibility.closeButton)
            .firstMatch
    }

    var activeCount: XCUIElement {
        element
            .textViews
            .matching(identifier: FiltersAccessibility.activeCount)
            .firstMatch
    }

    var dropButton: XCUIElement {
        element
            .buttons
            .matching(identifier: FiltersAccessibility.dropButton)
            .firstMatch
    }

    var category: CategoryPage {
        let elem = element
            .cells
            .matching(identifier: FiltersAccessibility.category)
            .firstMatch
        return CategoryPage(element: elem)
    }

    var snippets: [FiltersSnippetPage] {
        element
            .cells
            .matching(identifier: FiltersSnippetAccessibility.root)
            .allElementsBoundByIndex
            .map(FiltersSnippetPage.init)
    }

    var doneButton: DoneButtonPage {
        let elem = element
            .buttons
            .matching(identifier: FiltersAccessibility.doneButton)
            .firstMatch
        return DoneButtonPage(element: elem)
    }

    // MARK: - Public

    func snippet(named name: String) -> FiltersSnippetPage? {
        snippets.first { snippetPage -> Bool in
            snippetPage.name.label == name
        }
    }

}
