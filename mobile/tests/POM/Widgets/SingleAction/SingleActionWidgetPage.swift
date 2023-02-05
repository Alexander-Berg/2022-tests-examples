import XCTest

/// PageObject SingleAction-виджета с дженерик типом сниппета.
class SingleActionWidgetPage<CellPageType: PageObject>: PageObject {

    var snippet: CellPageType {
        CellPageType(element: element)
    }

}
