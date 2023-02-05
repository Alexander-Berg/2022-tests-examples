import Foundation
import UIUtils
import XCTest

class OpinionCreationPopupPage: PageObject, PopupPage {

    static let rootIdentifier = OpinionCreationPopupAccessibility.root

    var continueButton: XCUIElement {
        cellUniqueElement(withIdentifier: OpinionCreationPopupAccessibility.continueButton)
            .buttons
            .firstMatch
    }

    var cancelButton: XCUIElement {
        cellUniqueElement(withIdentifier: OpinionCreationPopupAccessibility.cancelButton)
            .buttons
            .firstMatch
    }

    func tapCancelButton() -> OpinionCreationSucceededPage {
        cancelButton.tap()

        let element = XCUIApplication().any.matching(identifier: OpinionCreationSucceededAccessibility.root).firstMatch
        return OpinionCreationSucceededPage(element: element)
    }

}

// MARK: - CollectionViewPage

extension OpinionCreationPopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OpinionCreationPopupViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
