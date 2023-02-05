import XCTest

final class HomePlusPage: PageObject {

    static var current: HomePlusPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: PlusAccessibility.homeRoot)
            .firstMatch
        return HomePlusPage(element: elem)
    }
}
