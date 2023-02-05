import MarketUI
import XCTest

final class DeliveryProgressPage: PageObject {

    static var current: DeliveryProgressPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: DeliveryProgressAccessibility.root)
            .firstMatch
        return DeliveryProgressPage(element: elem)
    }
}
