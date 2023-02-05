import UIUtils
import XCTest

// MARK: - PartnerProgramPopupPage

final class PartnerProgramPopupPage: PageObject, PopupPage {

    static var rootIdentifier: String = PartnerProgramAccessibility.root

    static var current: PartnerProgramPopupPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: PartnerProgramAccessibility.root)
            .firstMatch
        return PartnerProgramPopupPage(element: elem)
    }

    var image: XCUIElement {
        cellUniqueElement(withIdentifier: PartnerProgramAccessibility.image)
            .images
            .firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: PartnerProgramAccessibility.title)
            .textViews
            .firstMatch
    }

    var description: XCUIElement {
        cellUniqueElement(withIdentifier: PartnerProgramAccessibility.description)
            .textViews
            .firstMatch
    }

    var button: XCUIElement {
        cellUniqueElement(withIdentifier: PartnerProgramAccessibility.button)
            .buttons
            .firstMatch
    }

    func openDetails() {
        button.tap()
    }
}

// MARK: - CollectionViewPage

extension PartnerProgramPopupPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = PartnerProgramCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
