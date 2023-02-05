import XCTest

class BarcodePage: PageObject {

    static var current: BarcodePage {
        let element = XCUIApplication().otherElements[BarcodeAccessibility.root]
        return BarcodePage(element: element)
    }

    var orderId: XCUIElement {
        cellUniqueElement(withIdentifier: BarcodeAccessibility.orderId).firstMatch
    }

    var barcodeImage: XCUIElement {
        cellUniqueElement(withIdentifier: BarcodeAccessibility.barcode).firstMatch
    }

    var code: XCUIElement {
        cellUniqueElement(withIdentifier: BarcodeAccessibility.code).firstMatch
    }

    var closeButton: XCUIElement {
        cellUniqueElement(withIdentifier: BarcodeAccessibility.closeButton).firstMatch
    }

}

// MARK: - CollectionViewPage

extension BarcodePage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = BarcodeCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
