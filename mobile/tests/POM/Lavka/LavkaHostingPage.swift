import XCTest

final class LavkaHostingPage: PageObject {

    // MARK: - Properties

    static var current: LavkaHostingPage {
        let item = XCUIApplication().otherElements[LavkaHostingAccessibility.root]
        return LavkaHostingPage(element: item)
    }

}
