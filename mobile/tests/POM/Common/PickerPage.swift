import XCTest

class PickerPage: PageObject {

    static var current: PickerPage {
        let elem = XCUIApplication().pickers.firstMatch
        return PickerPage(element: elem)
    }

    func pickerWheel(at index: Int) -> PickerWheelPage {
        let elem = element.pickerWheels.element(boundBy: index)
        return PickerWheelPage(element: elem)
    }
}
