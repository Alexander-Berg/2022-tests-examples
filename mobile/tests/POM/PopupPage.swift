import UIUtils
import XCTest

protocol PopupPage: PageObject {

    /// Стрелка в хедере попапа
    var popupArrowView: XCUIElement { get }

    /// Бекграунд попапа.
    var dimmingBackgroundView: XCUIElement { get }

    /// Идентификатор корневого `view`.
    /// По этому идентификатору будет создаваться `currentPopup`.
    static var rootIdentifier: String { get }
}

extension PopupPage {
    var titleLabel: XCUIElement {
        XCUIApplication()
            .staticTexts.matching(identifier: PopupEmdeddingAccessibility.navigationHeaderTitle)
            .firstMatch
    }

    var popupArrowView: XCUIElement {
        XCUIApplication()
            .otherElements.matching(identifier: PopupEmdeddingAccessibility.arrowView)
            .firstMatch
    }

    var dimmingBackgroundView: XCUIElement {
        XCUIApplication()
            .otherElements.matching(identifier: PopupEmdeddingAccessibility.dimmingView)
            .firstMatch
    }

    var text: XCUIElement {
        element.staticTexts.firstMatch
    }

    static var currentPopup: Self {
        let element = XCUIApplication().any.matching(identifier: Self.rootIdentifier).firstMatch
        return .init(element: element)
    }
}
