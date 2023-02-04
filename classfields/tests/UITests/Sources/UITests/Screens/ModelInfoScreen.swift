import XCTest
import Snapshots

final class ModelInfoScreen: BaseScreen, Scrollable {
    lazy var scrollableElement: XCUIElement = findAll(.scrollView).firstMatch

    lazy var complectationTab = find(by: "segmentControlSegmentLabel_2").element(boundBy: 0)
}
