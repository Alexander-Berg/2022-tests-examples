import XCTest

final class CheckoutShipmentPopupPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CheckoutShipmentPopupItemsCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    func shipmentItem(at index: Int) -> CheckoutShipmentPopupItemPage {
        let elem = cellUniqueElement(
            withIdentifier: CheckoutShipmentPopupViewControllerAccessibility.Item.root,
            index: index
        )
        return CheckoutShipmentPopupItemPage(element: elem)
    }

    func shipmentItems() -> [XCUIElement] {
        allCellUniqueElement(withIdentifier: CheckoutShipmentPopupViewControllerAccessibility.Item.root)
    }
}
