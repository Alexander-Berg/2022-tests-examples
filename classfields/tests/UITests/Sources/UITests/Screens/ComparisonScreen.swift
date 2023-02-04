import XCTest
import Snapshots

class ComparisonScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var differenceSwitch = findAll(.switch).firstMatch
    lazy var comparisonHeaderShadowView = find(by: "comparison_header_shadow_view").firstMatch

    func optionSection(name: String) -> XCUIElement { find(by: "section_\(name)").firstMatch }
}

final class ComplectationComparisonScreen: ComparisonScreen {
    lazy var disclaimerPopupTitle = find(by: "Применимость").firstMatch

    func complectationHeader(name: String) -> XCUIElement { find(by: "complectation_header_view_\(name)").firstMatch }

    func optionMark(name: String, column: Int) -> XCUIElement { find(by: "option_mark_\(name)_\(column)").firstMatch }
}

class PhotoHeaderComparisonScreen: ComparisonScreen {
    func header(column: Int) -> XCUIElement { find(by: "header_\(column)").firstMatch }

    func headerPinButton(column: Int) -> XCUIElement { find(by: "header_\(column)_pin").firstMatch }

    func headerRemoveButton(column: Int) -> XCUIElement { find(by: "header_\(column)_remove").firstMatch }

    func headerCallButton(column: Int) -> XCUIElement { find(by: "header_\(column)_call").firstMatch }

    func clickableCell(name: String, column: Int) -> XCUIElement { find(by: "special_text_\(name)_\(column)").firstMatch }
}

final class OffersComparisonScreen: PhotoHeaderComparisonScreen {
    lazy var title = find(by: "Сравнение").firstMatch
}
