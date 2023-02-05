import UIUtils
import XCTest

final class MedicineMapPage: PageObject {

    // MARK: - Properties

    static var current: MedicineMapPage {
        let element = XCUIApplication()
            .otherElements
            .matching(identifier: PharmaAccessibility.root)
            .firstMatch
        return MedicineMapPage(element: element)
    }

    var outletSelector: XCUIElement {
        element
            .otherElements
            .matching(identifier: SegmentedButtonViewAccessibility.root)
            .descendants(matching: .button)
            .element(boundBy: 0)
    }

    var serviceSelector: XCUIElement {
        element
            .otherElements
            .matching(identifier: SegmentedButtonViewAccessibility.root)
            .descendants(matching: .button)
            .element(boundBy: 1)
    }

}
