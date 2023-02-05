import UIUtils
import XCTest

class OpinionCreationSucceededPage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: OpinionCreationSucceededAccessibility.root).firstMatch
        return .init(element: element)
    }

    var plusBadgeCell: XCUIElement {
        cellUniqueElement(withIdentifier: OpinionCreationSucceededAccessibility.plusBadgeCell)
    }

    var plusLinkButton: XCUIElement {
        cellUniqueElement(withIdentifier: OpinionCreationSucceededAccessibility.plusLinkCell)
            .buttons
            .firstMatch
    }

    var succeededButton: XCUIElement {
        cellUniqueElement(withIdentifier: OpinionCreationSucceededAccessibility.succeededButton)
            .buttons
            .firstMatch
    }

    var referralButton: ReferralButton {
        let element = cellUniqueElement(withIdentifier: OpinionCreationSucceededAccessibility.referralButton)
            .buttons
            .firstMatch

        return ReferralButton(element: element)
    }

}

// MARK: - CollectionViewPage

extension OpinionCreationSucceededPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OpinionCreationSucceededViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
