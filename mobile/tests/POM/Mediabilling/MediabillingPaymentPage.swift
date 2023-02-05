import XCTest

final class MediabillingPaymentPage: PageObject {

    // MARK: - Properties

    static var current: MediabillingPaymentPage {
        let item = XCUIApplication().otherElements[MediabillingAccessibility.root]
        return MediabillingPaymentPage(element: item)
    }
}
