import Foundation
import UIUtils
import XCTest

/// PopUp для отзыва
class LeaveOpinionPopUp: PageObject, PopupPage {
    static let rootIdentifier = LeaveOpinionPopupAccessibility.root

    var starView: XCUIElement {
        element
            .otherElements
            .matching(identifier: LeaveOpinionPopupAccessibility.starView)
            .firstMatch
    }

    var firstOpinionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: LeaveOpinionPopupAccessibility.firstButton)
            .firstMatch
    }

    var gradeOnlyButton: XCUIElement {
        element
            .buttons
            .matching(identifier: LeaveOpinionPopupAccessibility.gradeOnlyButton)
            .firstMatch
    }

    var prosText: TextViewPage {
        TextViewPage(element: cellUniqueElement(withIdentifier: LeaveOpinionPopupAccessibility.prosCell))
    }

    var consText: TextViewPage {
        TextViewPage(element: cellUniqueElement(withIdentifier: LeaveOpinionPopupAccessibility.consCell))
    }

    var commentText: TextViewPage {
        TextViewPage(element: cellUniqueElement(withIdentifier: LeaveOpinionPopupAccessibility.commentCell))
    }

    var continueButton: XCUIElement {
        cellUniqueElement(withIdentifier: LeaveOpinionPopupAccessibility.continueButton)
            .buttons
            .firstMatch
    }

    func tapContinueButton() -> OpinionCreationMultifactorPage {
        continueButton.tap()

        let element = XCUIApplication().any.matching(identifier: OpinionCreationMultifactorAccesibility.root).firstMatch
        return OpinionCreationMultifactorPage(element: element)
    }

}

// MARK: - CollectionViewPage

extension LeaveOpinionPopUp: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = LeaveOpinionPopupViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
