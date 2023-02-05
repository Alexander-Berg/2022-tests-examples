import UIUtils
import XCTest

final class SimilarsFeedPage: PageObject {

    // MARK: - Properties

    static var rootIdentifier: String = SimilarsFeedAccessibility.root

    static var current: SimilarsFeedPage {
        let element = XCUIApplication().otherElements[SimilarsFeedAccessibility.root]
        return SimilarsFeedPage(element: element)
    }

    var navigationLabel: XCUIElement {
        XCUIApplication().staticTexts[SimilarsFeedAccessibility.navigationTitle].firstMatch
    }

    var missingItemImageView: XCUIElement {
        cellUniqueElement(withIdentifier: SimilarsFeedAccessibility.imageCell).images.firstMatch
    }

    var textLabel: XCUIElement {
        cellUniqueElement(withIdentifier: SimilarsFeedAccessibility.textLabelCell).textViews.firstMatch
    }

    var bottomButton: XCUIElement {
        element.buttons[SimilarsFeedAccessibility.bottomButton].firstMatch
    }

    // MARK: - Methods

    func snippetCell(at index: Int) -> SnippetPage {
        let element = cellUniqueElement(withIdentifier: SimilarsFeedAccessibility.snippetCell, index: index)
        return SnippetPage(element: element)
    }
}

// MARK: - CollectionViewPage

extension SimilarsFeedPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = SimilarsFeedCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
