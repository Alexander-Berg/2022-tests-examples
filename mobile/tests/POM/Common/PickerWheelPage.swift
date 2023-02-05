import FormKit
import XCTest

final class PickerWheelPage: PageObject {

    // MARK: - Properties

    static var current: PickerWheelPage {
        let elem = XCUIApplication().pickerWheels.firstMatch
        return PickerWheelPage(element: elem)
    }

    var value: String? {
        element.value as? String
    }

    // MARK: - Public

    func adjust(toPickerWheelValue value: String) {
        element.adjust(toPickerWheelValue: value)
        if element.text != value {
            adjust(toPickerWheelValue: value)
        }
    }

}
