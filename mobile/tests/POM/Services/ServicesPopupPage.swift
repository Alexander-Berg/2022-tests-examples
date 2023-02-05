import FormKit
import MarketUI
import UIUtils
import XCTest

class ServicesPopupPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = CartServiceTypeCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var saveButton: XCUIElement {
        collectionView.buttons
            .matching(identifier: CartServicesPopupAccessibility.saveButton)
            .firstMatch
    }

    var moreInfoLink: XCUIElement {
        collectionView.buttons
            .matching(identifier: CartServicesPopupAccessibility.moreInfoButton)
            .firstMatch
    }

    var selectedService: RadioButtonPage {
        let elem = element.cells.containing(.image, identifier: RadioButtonViewCellAccessibility.Selectability.selected)
            .element
        return RadioButtonPage(element: elem)
    }

    func selectService(at index: Int) {
        let service = cellUniqueElement(withIdentifier: RadioButtonViewCellAccessibility.root, index: index)
        service.tap()
    }
}
