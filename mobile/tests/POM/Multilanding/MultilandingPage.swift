import XCTest

final class MultilandingPage: PageObject {

    static var current: MultilandingPage {
        let el = XCUIApplication().otherElements[MultilandingAccessibility.root]
        return MultilandingPage(element: el)
    }

    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    func snippetAt(item: Int, section: Int) -> FeedSnippetPage {
        let indexPath = IndexPath(item: item, section: section)
        let element = cellElement(at: indexPath)
        return FeedSnippetPage(element: element)
    }

    func titleAt(item: Int, section: Int) -> XCUIElement {
        let indexPath = IndexPath(item: item, section: section)
        let element = cellElement(at: indexPath)
        return element.staticTexts.firstMatch
    }
}

// MARK: - CollectionViewPage

extension MultilandingPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MultilandingCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}

// MARK: - Nested types

extension MultilandingPage {

    class NavigationBar: NavigationBarPage {

        override var title: XCUIElement {
            element
                .staticTexts.matching(identifier: NavigationBarAccessibility.barTitle)
                .firstMatch
        }

        var shareButton: XCUIElement {
            element
                .buttons.matching(identifier: NavigationBarAccessibility.shareButton)
                .firstMatch
        }

        var wishlistButton: XCUIElement {
            element
                .buttons.matching(identifier: NavigationBarAccessibility.wishlistButton)
                .firstMatch
        }

        var comparisonButton: XCUIElement {
            element
                .buttons.matching(identifier: ComparisonButtonAccessibility.button)
                .firstMatch
        }
    }
}
