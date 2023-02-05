import XCTest

final class TrustPage: PageObject {

    // MARK: - Properties

    static var current: TrustPage {
        let elem = XCUIApplication().otherElements[TrustAccessibility.root]
        return TrustPage(element: elem)
    }

    var navigationBar: NavigationBarPage {
        NavigationBarPage(element: NavigationBarPage.current.element)
    }

    var errorView: BarrierViewPage {
        BarrierViewPage(element: element)
    }
}
