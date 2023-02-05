import UIUtils
import XCTest

class OpinionAgitationPopupPage: PageObject, PopupPage {
    static let rootIdentifier = OpinionAgitationPopupAccessibility.root

    func rateProduct() {
        element.otherElements.matching(identifier: RateViewAccessibility.root).firstMatch.tap()
    }
}
