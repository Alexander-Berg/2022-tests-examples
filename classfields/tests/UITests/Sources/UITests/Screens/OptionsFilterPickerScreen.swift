import XCTest
import Snapshots

final class OptionsFilterPickerScreen: BaseScreen, Scrollable {
    lazy var scrollableElement: XCUIElement = findAll(.scrollView).firstMatch

    lazy var comparisonButton = find(by: "Сравнить").firstMatch
}
