import FormKit
import MarketUI
import UIUtils
import XCTest

final class PromocodePopupPage: PageObject, PopupPage {
    static var rootIdentifier: String = PromocodePopupAccessibility.root

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.titleLabel).textViews.firstMatch
    }

    var info: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.infoButton).textViews.firstMatch
    }

    var messageLabel: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.messageLabel)
    }

    var basePrice: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.basePriceLabel)
            .staticTexts[HorizontalTitleDetailsAccessibility.details].firstMatch
    }

    var discount: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.promocodeDiscountLabel)
            .staticTexts[HorizontalTitleDetailsAccessibility.details].firstMatch
    }

    var totalPrice: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.totalPriceLabel)
            .staticTexts[HorizontalTitleDetailsAccessibility.details].firstMatch
    }

    var addToCart: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.addToCartButton).buttons.firstMatch
    }

    var showAllWithPromocode: XCUIElement {
        cellUniqueElement(withIdentifier: PromocodePopupAccessibility.showAllProductsWithPromocodeButton).buttons
            .firstMatch
    }
}

// MARK: - CollectionViewPage

extension PromocodePopupPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = PromocodePopupCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
