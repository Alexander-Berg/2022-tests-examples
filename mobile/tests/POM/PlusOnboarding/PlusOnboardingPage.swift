import UIUtils
import XCTest

final class PlusOnboardingPage: PageObject {

    // MARK: - Properties

    static var current: PlusOnboardingPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: PlusOnboardingAccessibility.root)
            .firstMatch
        return PlusOnboardingPage(element: elem)
    }

    var image: XCUIElement {
        cellUniqueElement(withIdentifier: PlusOnboardingAccessibility.imageView)
            .firstMatch
            .images
            .firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: PlusOnboardingAccessibility.title)
            .firstMatch
            .textViews
            .firstMatch
    }

    var text: XCUIElement {
        cellUniqueElement(withIdentifier: PlusOnboardingAccessibility.text)
            .firstMatch
            .textViews
            .firstMatch
    }

    var button: XCUIElement {
        cellUniqueElement(withIdentifier: PlusOnboardingAccessibility.button)
            .firstMatch
            .buttons
            .firstMatch
    }

}

// MARK: - CollectionViewPage

extension PlusOnboardingPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = PlusOnboardingCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - PopupPage

extension PlusOnboardingPage: PopupPage {

    static let rootIdentifier = PlusOnboardingAccessibility.root

}
