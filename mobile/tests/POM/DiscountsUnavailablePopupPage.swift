import XCTest

final class DiscountsUnavailablePopupPage: PageObject, PopupPage {

    static var rootIdentifier = DiscountsUnavailablePopupAccessibility.root

    /// Заголовок
    var titleLabel: XCUIElement {
        cellUniqueElement(withIdentifier: DiscountsUnavailablePopupAccessibility.titleLabel)
            .textViews
            .firstMatch
    }

    /// Текст
    var messageLabel: XCUIElement {
        cellUniqueElement(withIdentifier: DiscountsUnavailablePopupAccessibility.messageLabel)
            .textViews
            .firstMatch
    }

    /// Кнопка
    var actionButton: XCUIElement {
        cellUniqueElement(withIdentifier: DiscountsUnavailablePopupAccessibility.actionButton)
            .buttons
            .firstMatch
    }
}

// MARK: - CollectionViewPage

extension DiscountsUnavailablePopupPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = DiscountsPopupCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
