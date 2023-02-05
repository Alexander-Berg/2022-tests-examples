import XCTest

final class PromoHubPage: PageObject {

    // MARK: - Properties

    var pushSubscribe: PushSubscribe {
        let el = cellUniqueElement(withIdentifier: PushSubscribeAccessibility.root)
        return PushSubscribe(element: el)
    }

}

// MARK: - CollectionViewPage

extension PromoHubPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = PromoHubCollectionViewCellAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}
