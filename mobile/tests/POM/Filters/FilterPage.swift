import UIUtils
import XCTest

class FilterPage: PageObject {

    // MARK: - Properties

    var countSnippet: FiltersActiveCountCellPage {
        let elem = element
            .cells
            .matching(identifier: FilterActiveCountCellAccessibility.root)
            .firstMatch

        return FiltersActiveCountCellPage(element: elem)
    }

    var snippets: [FiltersSnippetPage] {
        element
            .cells
            .matching(identifier: FiltersSnippetAccessibility.root)
            .allElementsBoundByIndex
            .map(FiltersSnippetPage.init)
    }

    var doneButton: XCUIElement {
        element
            .buttons
            .matching(identifier: FilterAccessibility.doneButton)
            .firstMatch
    }

    // MARK: - Public

    func snippet(named name: String) -> FiltersSnippetPage? {
        snippets.first { snippetPage -> Bool in
            snippetPage.name.label.trimmingCharacters(in: .whitespacesAndNewlines) == name
        }
    }

}
