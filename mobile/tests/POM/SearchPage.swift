import XCTest

class SearchPage: PageObject {
    /// Класс ячейки саджеста по истории поиска
    class SuggestCell: PageObject, SKUEntryPoint {
        var text: XCUIElement {
            element
                .staticTexts
                .matching(identifier: SearchAccessibility.suggestCellText)
                .firstMatch
        }
    }

    static var current: SearchPage {
        let el = XCUIApplication().any[SearchAccessibility.root]
        return SearchPage(element: el)
    }

    class SearchNavigationBarPage: NavigationBarPage {
        var searchTextField: XCUIElement {
            XCUIApplication().searchFields[NavigationBarAccessibility.searchTextField]
        }
    }

    var navigationBar: SearchNavigationBarPage {
        SearchNavigationBarPage(element: NavigationBarPage.current.element)
    }

    /// Ячейка чипсинов (дополнений запроса)
    var chipsCell: XCUIElement {
        element.cells.matching(identifier: SearchAccessibility.chipsCell).firstMatch
    }

    /// Все ячейки истории поиска
    var suggestsCells: [SuggestCell] {
        element
            .cells
            .matching(identifier: SearchAccessibility.suggestCell)
            .allElementsBoundByIndex
            .map { SuggestCell(element: $0) }
    }

    /// Ячейка "Очистить историю"
    var clearHistoryButton: XCUIElement {
        element
            .cells
            .matching(identifier: SearchAccessibility.clearHistoryButton)
            .firstMatch
            .staticTexts
            .firstMatch
    }

    /// Ячейка нулевого саджеста
    var zeroSuggestTitle: XCUIElement {
        element.staticTexts.matching(identifier: SearchAccessibility.zeroSuggestTitle).firstMatch
    }

    var zeroSuggestSubtitle: XCUIElement {
        element.staticTexts.matching(identifier: SearchAccessibility.zeroSuggestSubtitle).firstMatch
    }
}
