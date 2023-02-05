import UIUtils
import XCTest

class OpinionCreationMultifactorPage: PageObject {

    var continueButton: XCUIElement {
        cellUniqueElement(withIdentifier: LeaveOpinionPopupAccessibility.continueButton)
            .buttons
            .firstMatch
    }

    func tapContinueButton() -> OpinionCreationSucceededPage {
        continueButton.tap()

        let element = XCUIApplication().any.matching(identifier: OpinionCreationSucceededAccessibility.root).firstMatch
        return OpinionCreationSucceededPage(element: element)
    }

    func tapCloseButton() {
        NavigationBarPage.current.closeButton.tap()
    }

}

// MARK: - CollectionViewPage

extension OpinionCreationMultifactorPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OpinionCreationMultifactorViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
