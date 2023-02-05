import FormKit
import MarketUI
import XCTest

class FlowPopupPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = FlowPopupCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var firstElement: XCUIElement {
        cellElement(at: IndexPath(item: 0, section: 0))
    }

    var firstSuggest: Suggest {
        Suggest(element: firstElement)
    }

    func suggest(index: Int) -> Suggest {
        let el = cellElement(at: IndexPath(item: index, section: 0))
        return Suggest(element: el)
    }

    var allAddressess: [Suggest] {
        allCellUniqueElement(withIdentifier: FlowPopupAccessibility.streetAddress).map(Suggest.init(element:))
    }

    var allSuggests: [Suggest] {
        allCellUniqueElement(withIdentifier: FlowPopupAccessibility.streetSuggest).map(Suggest.init(element:))
    }

}

// MARK: - Nested types

extension FlowPopupPage {

    class Suggest: PageObject {
        var textView: XCUIElement { element.textViews.firstMatch }
        var image: XCUIElement { element.images.firstMatch }
    }

}
