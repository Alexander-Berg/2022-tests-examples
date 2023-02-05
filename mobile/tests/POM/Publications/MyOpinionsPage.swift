import UIUtils
import XCTest

final class MyOpinionsPage: PageObject {

    // MARK: - Properties

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: MyOpinionsAccessibility.root).firstMatch
        return .init(element: element)
    }

    var toOrdersButton: XCUIElement {
        element
            .buttons
            .matching(identifier: BarrierViewAccessibility.actionButton)
            .firstMatch
    }

    var openContextMenuButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyOpinionsAccessibility.modelInfoCell)
            .buttons
            .firstMatch
    }

    var modelInfoCell: XCUIElement {
        cellUniqueElement(withIdentifier: MyOpinionsAccessibility.modelInfoCell)
    }

    var reviewCell: XCUIElement {
        cellUniqueElement(withIdentifier: MyOpinionsAccessibility.reviewCell)
    }

    var reviewText: XCUIElement {
        reviewCell
            .textViews
            .firstMatch
    }

    var moderationStateCell: XCUIElement {
        cellUniqueElement(withIdentifier: MyOpinionsAccessibility.moderationStateCell)
    }

}

// MARK: - CollectionViewPage

extension MyOpinionsPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MyOpinionsCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
